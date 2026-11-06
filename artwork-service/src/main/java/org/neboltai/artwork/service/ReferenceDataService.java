package org.neboltai.artwork.service;

import org.neboltai.artwork.repository.AuthorRepository;
import org.neboltai.artwork.repository.MediaRepository;
import org.neboltai.artwork.repository.MediaTypeRepository;
import org.neboltai.artwork.dto.AddAuthorRequest;
import org.neboltai.artwork.dto.AddMediaRequest;
import org.neboltai.artwork.dto.AddMediaTypeRequest;
import org.neboltai.artwork.entity.Media;
import org.neboltai.artwork.entity.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.neboltai.artwork.dto.AuthorDto;
import org.neboltai.artwork.dto.NamedItemDto;
import org.neboltai.artwork.dto.MediaTypeDto;
import org.neboltai.artwork.entity.Author;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferenceDataService {
    private final AuthorRepository authorRepository;
    private final MediaRepository mediaRepository;
    private final MediaTypeRepository mediaTypeRepository;

    public List<AuthorDto> authors() {
        return authorRepository.findAll().stream()
                .map(a -> new AuthorDto(a.getId(),
                        Stream.of(a.getFamilyName(), a.getName(), a.getSecondName())
                                .filter(part -> part != null && !part.isBlank())
                                .map(String::strip)
                                .collect(Collectors.joining(" "))))
                .sorted(Comparator.comparing(AuthorDto::author).thenComparing(AuthorDto::id))
                .toList();
    }

    public List<NamedItemDto> medias() {
        return mediaRepository.findAllItems();
    }

    public List<MediaTypeDto> mediaTypes() {
        return mediaTypeRepository.findAllItems();
    }

    @Transactional
    public AuthorDto addAuthor(AddAuthorRequest request) {
        Author author = Author.builder()
                .familyName(requiredText(request.familyName(), "familyName"))
                .name(optionalText(request.name(), "name", 256))
                .secondName(optionalText(request.secondName(), "secondName", 256))
                .description(optionalText(request.description(), "description", 4000))
                .build();
        author = authorRepository.save(author);
        String fullName = Stream.of(author.getFamilyName(), author.getName(), author.getSecondName())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return new AuthorDto(author.getId(), fullName);
    }

    @Transactional
    public NamedItemDto addMedia(AddMediaRequest request) {
        String name = requiredText(request.name(), "name");
        if (request.typeId() == null || request.typeId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "typeId must be positive");
        }
        MediaType type = mediaTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media type not found"));
        Media media = Media.builder().name(name).type(type)
                .description(optionalText(request.description(), "description", 4000)).build();
        media = mediaRepository.save(media);
        return new NamedItemDto(media.getId(), media.getName());
    }

    @Transactional
    public MediaTypeDto addMediaType(AddMediaTypeRequest request) {
        String typeName = requiredText(request.type(), "type");
        MediaType type = MediaType.builder().type(typeName)
                .description(optionalText(request.description(), "description", 4000)).build();
        type = mediaTypeRepository.save(type);
        return new MediaTypeDto(type.getId(), type.getType());
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must not be blank");
        }
        return optionalText(value, field, 256);
    }

    private static String optionalText(String value, String field, int maxLength) {
        if (value == null) {
            return null;
        }
        String text = value.strip();
        if (text.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " must contain at most " + maxLength + " characters");
        }
        return text;
    }
}
