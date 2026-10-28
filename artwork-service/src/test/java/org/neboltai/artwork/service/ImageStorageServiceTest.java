package org.neboltai.artwork.service;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageStorageServiceTest {
    private final MinioClient client = mock(MinioClient.class);
    private final ImageStorageService service = new ImageStorageService(client);

    @Test
    void checksMasterBucketWithUuidObjectKey() throws Exception {
        ReflectionTestUtils.setField(service, "masterBucket", "collection-masters");
        UUID id = UUID.randomUUID();
        service.requireImage(id);
        var captor = org.mockito.ArgumentCaptor.forClass(StatObjectArgs.class);
        verify(client).statObject(captor.capture());
        assertEquals("collection-masters", captor.getValue().bucket());
        assertEquals(id.toString(), captor.getValue().object());
    }

    @Test
    void missingObjectReturns404() throws Exception {
        ReflectionTestUtils.setField(service, "masterBucket", "collection-masters");
        ErrorResponse response = mock(ErrorResponse.class);
        when(response.code()).thenReturn("NoSuchKey");
        ErrorResponseException missing = new ErrorResponseException(response, null, "not found");
        when(client.statObject(any(StatObjectArgs.class))).thenThrow(missing);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.requireImage(UUID.randomUUID())).getStatusCode());
    }

    @Test
    void storageFailureReturns503() throws Exception {
        ReflectionTestUtils.setField(service, "masterBucket", "collection-masters");
        when(client.statObject(any(StatObjectArgs.class))).thenThrow(new java.io.IOException("offline"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
                () -> service.requireImage(UUID.randomUUID())).getStatusCode());
    }

    @Test
    void deletesOriginalAndPreviewWithSameUuid() throws Exception {
        ReflectionTestUtils.setField(service, "masterBucket", "collection-masters");
        ReflectionTestUtils.setField(service, "previewBucket", "collection-previews");
        UUID id = UUID.randomUUID();
        service.deleteImage(id);
        var captor = org.mockito.ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(client, times(2)).removeObject(captor.capture());
        assertEquals("collection-masters", captor.getAllValues().get(0).bucket());
        assertEquals("collection-previews", captor.getAllValues().get(1).bucket());
        captor.getAllValues().forEach(args -> assertEquals(id.toString(), args.object()));
        verify(client, never()).statObject(any());
    }

    @Test
    void previewDeletionFailureReturns503() throws Exception {
        ReflectionTestUtils.setField(service, "masterBucket", "collection-masters");
        ReflectionTestUtils.setField(service, "previewBucket", "collection-previews");
        doNothing().doThrow(new java.io.IOException("offline")).when(client).removeObject(any());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
                () -> service.deleteImage(UUID.randomUUID())).getStatusCode());
    }
}
