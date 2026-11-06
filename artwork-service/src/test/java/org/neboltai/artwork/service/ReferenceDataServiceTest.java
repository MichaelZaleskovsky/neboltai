package org.neboltai.artwork.service;

import org.neboltai.artwork.repository.AuthorRepository;
import org.neboltai.artwork.repository.MediaRepository;
import org.neboltai.artwork.repository.MediaTypeRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.neboltai.artwork.dto.*;
import org.neboltai.artwork.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReferenceDataServiceTest {
    private final AuthorRepository authorRepository = mock(AuthorRepository.class);
    private final MediaRepository mediaRepository = mock(MediaRepository.class);
    private final MediaTypeRepository mediaTypeRepository = mock(MediaTypeRepository.class);
    private final ReferenceDataService service = new ReferenceDataService(
            authorRepository, mediaRepository, mediaTypeRepository);

    @Test
    void createsAuthorWithGeneratedIdAndFormattedName() {
        doAnswer(call -> {
            Author author = call.getArgument(0);
            author.setId(42L);
            return author;
        }).when(authorRepository).save(any(Author.class));
        AuthorDto result = service.addAuthor(new AddAuthorRequest(" Ivanov ", " Ivan ", null, null));
        assertEquals(new AuthorDto(42L, "Ivanov Ivan"), result);
    }

    @Test
    void createsMediaWithExistingType() {
        MediaType type = MediaType.builder().id(7L).type("Book").build();
        when(mediaTypeRepository.findById(7L)).thenReturn(Optional.of(type));
        doAnswer(call -> {
            Media media = call.getArgument(0);
            assertSame(type, media.getType());
            media.setId(8L);
            return media;
        }).when(mediaRepository).save(any(Media.class));
        assertEquals(new NamedItemDto(8L, "Album"),
                service.addMedia(new AddMediaRequest(" Album ", 7L, null)));
    }

    @Test
    void createsMediaTypeWithGeneratedId() {
        doAnswer(call -> {
            MediaType type = call.getArgument(0);
            type.setId(7L);
            return type;
        }).when(mediaTypeRepository).save(any(MediaType.class));
        assertEquals(new MediaTypeDto(7L, "Book"),
                service.addMediaType(new AddMediaTypeRequest(" Book ", null)));
    }

    @Test
    void rejectsMissingMediaTypeWithoutPersisting() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.addMedia(new AddMediaRequest("Album", 99L, null)));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(authorRepository, never()).save(any());
        verify(mediaRepository, never()).save(any());
        verify(mediaTypeRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidInputWithoutPersisting() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.addAuthor(new AddAuthorRequest(" ", null, null, null))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.addMedia(new AddMediaRequest("Album", null, null))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.addMediaType(new AddMediaTypeRequest("a".repeat(257), null))).getStatusCode());
        verify(authorRepository, never()).save(any());
        verify(mediaRepository, never()).save(any());
        verify(mediaTypeRepository, never()).save(any());
    }
}
