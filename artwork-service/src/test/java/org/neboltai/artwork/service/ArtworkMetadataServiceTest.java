package org.neboltai.artwork.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neboltai.artwork.dto.AddMediaDataRequest;
import org.neboltai.artwork.entity.*;
import org.neboltai.artwork.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArtworkMetadataServiceTest {
    private final ArtworkRepository artworks = mock(ArtworkRepository.class);
    private final AuthorRepository authors = mock(AuthorRepository.class);
    private final MediaRepository medias = mock(MediaRepository.class);
    private final ImageStorageService storage = mock(ImageStorageService.class);
    private final ArtworkDtoMapper mapper = mock(ArtworkDtoMapper.class);
    private final ArtworkMetadataService service = new ArtworkMetadataService(artworks, authors, medias, storage, mapper);
    private final UUID imageId = UUID.randomUUID();
    private final Author author = Author.builder().id(1L).build();
    private final Media media = Media.builder().id(2L).build();

    @BeforeEach
    void setup() {
        when(artworks.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void createsMetadataForExistingImage() {
        when(authors.findById(1L)).thenReturn(Optional.of(author));
        when(medias.findById(2L)).thenReturn(Optional.of(media));
        service.addMediaData(new AddMediaDataRequest(imageId, " Title ", "image.jpg", 2L,
                100, 200, 2020, " Description ", 1L));
        var captor = org.mockito.ArgumentCaptor.forClass(Artwork.class);
        verify(artworks).save(captor.capture());
        Artwork created = captor.getValue();
        assertEquals(imageId, created.getImageId());
        assertEquals("Title", created.getImageName());
        assertEquals("Description", created.getDescription());
        assertSame(author, created.getAuthor());
        assertSame(media, created.getMedia());
        verify(storage).requireImage(imageId);
        verify(mapper).toDetail(created);
    }

    @Test
    void updatesOnlyNonEmptyFields() {
        Artwork existing = existing();
        service.addMediaData(new AddMediaDataRequest(imageId, " New title ", "  ", null,
                null, null, 2024, "", null));
        assertEquals("New title", existing.getImageName());
        assertEquals("old.jpg", existing.getFileName());
        assertEquals("Old description", existing.getDescription());
        assertEquals(100, existing.getWidth());
        assertEquals(200, existing.getHeight());
        assertEquals(2024, existing.getYear());
        assertSame(author, existing.getAuthor());
        assertSame(media, existing.getMedia());
        verify(artworks).save(existing);
        verifyNoInteractions(authors, medias);
    }

    @Test
    void missingImagePreventsDatabaseAccessEvenForExistingMetadata() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(storage).requireImage(imageId);
        var error = assertThrows(ResponseStatusException.class, () -> service.addMediaData(
                new AddMediaDataRequest(imageId, null, null, null, null, null, null, null, null)));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(artworks, authors, medias, mapper);
    }

    @Test
    void rejectsIncompleteNewMetadata() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.addMediaData(new AddMediaDataRequest(imageId, "Title", null,
                        null, null, null, null, null, null))).getStatusCode());
        verify(artworks, never()).save(any());
    }

    @Test
    void rejectsUnknownReferenceAndInvalidDimensions() {
        existing();
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.addMediaData(new AddMediaDataRequest(imageId, null, null,
                        99L, null, null, null, null, null))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.addMediaData(new AddMediaDataRequest(imageId, null, null,
                        null, 0, null, null, null, null))).getStatusCode());
        verify(artworks, never()).save(any());
    }

    @Test
    void imageIdIsRequired() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.addMediaData(new AddMediaDataRequest(null, null, null,
                        null, null, null, null, null, null))).getStatusCode());
        verifyNoInteractions(storage, artworks);
    }

    private Artwork existing() {
        Artwork artwork = Artwork.builder().id(9L).imageId(imageId).imageName("Old title")
                .fileName("old.jpg").width(100).height(200).year(2020)
                .description("Old description").author(author).media(media).build();
        when(artworks.findByImageId(imageId)).thenReturn(Optional.of(artwork));
        return artwork;
    }
}
