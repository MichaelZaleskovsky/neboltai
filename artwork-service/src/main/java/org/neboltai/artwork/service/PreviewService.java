package org.neboltai.artwork.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/** Reads small preview copies (JPEG) stored in MinIO as {@code <previewBucket>/<uuid>}. */
@Service
@RequiredArgsConstructor
public class PreviewService {
    private final MinioClient minioClient;

    @Value("${app.minio.preview-bucket}")
    private String previewBucket;

    /** Returns JPEG bytes, or empty if the preview does not exist. */
    public Optional<byte[]> findPreview(UUID id) {
        try (InputStream in = minioClient.getObject(
                GetObjectArgs.builder().bucket(previewBucket).object(id.toString()).build())) {
            return Optional.of(in.readAllBytes());
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return Optional.empty();
            }
            throw new IllegalStateException("Cannot read preview " + id, e);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read preview " + id, e);
        }
    }
}
