package org.neboltai.artwork.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageUploadServiceTest {
    private final MinioClient client = mock(MinioClient.class);
    private final PyramidalTiffConverter converter = mock(PyramidalTiffConverter.class);
    private final ImageUploadService service = new ImageUploadService(client, converter);

    @BeforeEach
    void convertedFixture() {
        doAnswer(call -> {
            Files.write(call.getArgument(1, Path.class), image("tiff", 16, 8));
            return null;
        }).when(converter).convert(any(), any());
    }

    ImageUploadServiceTest() {
        ReflectionTestUtils.setField(service, "masterBucket", "collection-masters");
        ReflectionTestUtils.setField(service, "previewBucket", "collection-previews");
        ReflectionTestUtils.setField(service, "publicBaseUrl", "http://localhost:8080/");
    }

    @Test
    void uploadsJpegAndLandscapePreview() throws Exception {
        verifyUpload("jpeg", 1200, 800, 200);
    }

    @Test
    void uploadsTiffAndPortraitPreview() throws Exception {
        verifyUpload("tiff", 800, 1200, 450);
    }

    @Test
    void previewIsExactly300PixelsEvenForSmallImages() throws Exception {
        verifyUpload("jpeg", 150, 100, 200);
    }

    private void verifyUpload(String format, int width, int height, int previewHeight) throws Exception {
        byte[] original = image(format, width, height);
        List<PutObjectArgs> uploads = new ArrayList<>();
        List<byte[]> contents = new ArrayList<>();
        when(client.putObject(any())).thenAnswer(call -> {
            PutObjectArgs args = call.getArgument(0);
            uploads.add(args);
            contents.add(args.stream().readAllBytes());
            return null;
        });
        var result = service.addImage(new MockMultipartFile("file", "photo." + format,
                "application/octet-stream", original));
        assertNotNull(result.imageId());
        assertEquals(width, result.width());
        assertEquals(height, result.height());
        assertEquals(300, result.previewWidth());
        assertEquals(previewHeight, result.previewHeight());
        assertEquals("http://localhost:8080/api/artworks/preview/" + result.imageId(), result.previewUrl());
        assertEquals(2, uploads.size());
        assertEquals("collection-masters", uploads.get(0).bucket());
        assertEquals("collection-previews", uploads.get(1).bucket());
        assertEquals(result.imageId().toString(), uploads.get(0).object());
        assertEquals(uploads.get(0).object(), uploads.get(1).object());
        assertFalse(java.util.Arrays.equals(original, contents.get(0)));
        assertArrayEquals(image("tiff", 16, 8), contents.get(0));
        assertEquals("image/tiff", uploads.get(0).contentType());
        var inputPath = org.mockito.ArgumentCaptor.forClass(Path.class);
        var outputPath = org.mockito.ArgumentCaptor.forClass(Path.class);
        verify(converter).convert(inputPath.capture(), outputPath.capture());
        assertFalse(Files.exists(inputPath.getValue()));
        assertFalse(Files.exists(outputPath.getValue()));
        assertFalse(Files.exists(inputPath.getValue().getParent()));
        BufferedImage preview = ImageIO.read(new ByteArrayInputStream(contents.get(1)));
        assertEquals(300, preview.getWidth());
        assertEquals(previewHeight, preview.getHeight());
        verify(client, never()).removeObject(any());
    }

    @Test
    void rejectsEmptyAndUnsupportedFilesWithoutUploading() throws Exception {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.addImage(new MockMultipartFile("file", new byte[0]))).getStatusCode());
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, assertThrows(ResponseStatusException.class,
                () -> service.addImage(new MockMultipartFile("file", "fake.jpg", "image/jpeg",
                        image("png", 20, 20)))).getStatusCode());
        verifyNoInteractions(client);
    }

    @Test
    void rollsBackObjectsWhenPreviewUploadFails() throws Exception {
        when(client.putObject(any())).thenReturn(null).thenThrow(new IOException("offline"));
        var error = assertThrows(ResponseStatusException.class,
                () -> service.addImage(new MockMultipartFile("file", image("jpeg", 600, 400))));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        var removed = org.mockito.ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(client, times(2)).removeObject(removed.capture());
        assertEquals("collection-previews", removed.getAllValues().get(0).bucket());
        assertEquals("collection-masters", removed.getAllValues().get(1).bucket());
        assertEquals(removed.getAllValues().get(0).object(), removed.getAllValues().get(1).object());
    }

    @Test
    void conversionFailureUploadsNothingAndRemovesTemporaryInput() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid TIFF"))
                .when(converter).convert(any(), any());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.addImage(new MockMultipartFile("file", image("jpeg", 600, 400)))).getStatusCode());
        verifyNoInteractions(client);
        var inputPath = org.mockito.ArgumentCaptor.forClass(Path.class);
        verify(converter).convert(inputPath.capture(), any());
        assertFalse(Files.exists(inputPath.getValue()));
        assertFalse(Files.exists(inputPath.getValue().getParent()));
    }

    private static byte[] image(String format, int width, int height) throws IOException {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, output));
        return output.toByteArray();
    }
}
