package org.neboltai.artwork.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.neboltai.artwork.dto.AddImageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageUploadService {
    private static final int PREVIEW_WIDTH = 300;
    private final MinioClient minioClient;

    @Value("${app.minio.master-bucket}")
    private String masterBucket;
    @Value("${app.minio.preview-bucket}")
    private String previewBucket;
    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    public AddImageResponse addImage(MultipartFile file) {
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is empty");
        Path temporary = null;
        UUID imageId = UUID.randomUUID();
        boolean uploadStarted = false;
        try {
            temporary = Files.createTempFile("artwork-upload-", ".image");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temporary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            try (ImageInputStream input = ImageIO.createImageInputStream(temporary.toFile())) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw unsupported();
                ImageReader reader = readers.next();
                try {
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (format.equals("tif")) format = "tiff";
                    if (format.equals("jpg")) format = "jpeg";
                    if (!format.equals("jpeg") && !format.equals("tiff")) throw unsupported();
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    int previewHeight = Math.max(1, (int) Math.round(height * (double) PREVIEW_WIDTH / width));
                    // Subsample while decoding so large masters do not require a full-size bitmap in memory.
                    var parameters = reader.getDefaultReadParam();
                    int sample = Math.max(1, width / PREVIEW_WIDTH);
                    parameters.setSourceSubsampling(sample, sample, 0, 0);
                    BufferedImage source;
                    try {
                        source = reader.read(0, parameters);
                    } catch (IOException | IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot decode image", e);
                    }
                    uploadStarted = true;
                    try (InputStream original = Files.newInputStream(temporary)) {
                        minioClient.putObject(PutObjectArgs.builder().bucket(masterBucket)
                                .object(imageId.toString()).stream(original, Files.size(temporary), -1)
                                .contentType(format.equals("jpeg") ? "image/jpeg" : "image/tiff").build());
                    }
                    byte[] preview = preview(source, previewHeight);
                    try (InputStream bytes = new ByteArrayInputStream(preview)) {
                        minioClient.putObject(PutObjectArgs.builder().bucket(previewBucket)
                                .object(imageId.toString()).stream(bytes, preview.length, -1)
                                .contentType("image/jpeg").build());
                    }
                    return new AddImageResponse(imageId, file.getOriginalFilename(), width, height,
                            PREVIEW_WIDTH, previewHeight,
                            publicBaseUrl.replaceAll("/$", "") + "/api/artworks/preview/" + imageId);
                } finally {
                    reader.dispose();
                }
            }
        } catch (ResponseStatusException e) {
            if (uploadStarted) cleanup(imageId, e);
            throw e;
        } catch (Exception e) {
            if (uploadStarted) cleanup(imageId, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cannot upload image", e);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private static byte[] preview(BufferedImage source, int height) throws IOException {
        BufferedImage result = new BufferedImage(PREVIEW_WIDTH, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, PREVIEW_WIDTH, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source, 0, 0, PREVIEW_WIDTH, height, null);
        } finally {
            graphics.dispose();
            source.flush();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(result, "jpeg", output)) throw new IOException("JPEG writer unavailable");
            return output.toByteArray();
        } finally {
            result.flush();
        }
    }

    private void cleanup(UUID imageId, Exception failure) {
        for (String bucket : new String[]{previewBucket, masterBucket}) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(imageId.toString()).build());
            } catch (Exception cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
    }

    private static ResponseStatusException unsupported() {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only TIFF and JPEG images are supported");
    }
}
