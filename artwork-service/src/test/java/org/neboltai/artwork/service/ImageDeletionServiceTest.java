package org.neboltai.artwork.service;

import org.junit.jupiter.api.Test;
import org.neboltai.artwork.repository.ArtworkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageDeletionServiceTest {
    private final ImageStorageService storage = mock(ImageStorageService.class);
    private final ArtworkRepository repository = mock(ArtworkRepository.class);
    private final ImageDeletionService service = new ImageDeletionService(storage, repository);

    @Test
    void deletesObjectsBeforeMetadata() {
        UUID id = UUID.randomUUID();
        service.deleteImage(id);
        var order = inOrder(storage, repository);
        order.verify(storage).deleteImage(id);
        order.verify(repository).deleteByImageId(id);
    }

    @Test
    void missingMetadataDoesNotPreventRepeatedDeletion() {
        UUID id = UUID.randomUUID();
        when(repository.deleteByImageId(id)).thenReturn(0L);
        service.deleteImage(id);
        service.deleteImage(id);
        verify(storage, times(2)).deleteImage(id);
        verify(repository, times(2)).deleteByImageId(id);
    }

    @Test
    void storageFailurePreservesMetadata() {
        UUID id = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).when(storage).deleteImage(id);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class, () -> service.deleteImage(id)).getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void missingUuidIsRejected() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> service.deleteImage(null)).getStatusCode());
        verifyNoInteractions(storage, repository);
    }
}
