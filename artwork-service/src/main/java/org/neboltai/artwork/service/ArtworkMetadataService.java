package org.neboltai.artwork.service;

import lombok.RequiredArgsConstructor;
import org.neboltai.artwork.dto.AddMediaDataRequest;
import org.neboltai.artwork.dto.ArtworkDetailDto;
import org.neboltai.artwork.entity.Artwork;
import org.neboltai.artwork.repository.ArtworkRepository;
import org.neboltai.artwork.repository.AuthorRepository;
import org.neboltai.artwork.repository.MediaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ArtworkMetadataService {
    private final ArtworkRepository artworkRepository;
    private final AuthorRepository authorRepository;
    private final MediaRepository mediaRepository;
    private final ImageStorageService imageStorageService;
    private final ArtworkDtoMapper mapper;

    @Transactional
    public ArtworkDetailDto addMediaData(AddMediaDataRequest request) {
        if (request.imageId() == null) {
            throw badRequest("imageId is required");
        }
        imageStorageService.requireImage(request.imageId());
        Artwork artwork = artworkRepository.findByImageId(request.imageId())
                .orElseGet(() -> Artwork.builder().imageId(request.imageId()).build());
        if (hasText(request.imageName())) artwork.setImageName(text(request.imageName(), "imageName", 256));
        if (hasText(request.fileName())) artwork.setFileName(text(request.fileName(), "fileName", 256));
        if (hasText(request.description())) artwork.setDescription(text(request.description(), "description", 4000));
        if (request.width() != null) artwork.setWidth(positive(request.width(), "width"));
        if (request.height() != null) artwork.setHeight(positive(request.height(), "height"));
        if (request.year() != null) artwork.setYear(request.year());
        if (request.authorId() != null) {
            positive(request.authorId(), "authorId");
            artwork.setAuthor(authorRepository.findById(request.authorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found")));
        }
        if (request.mediaId() != null) {
            positive(request.mediaId(), "mediaId");
            artwork.setMedia(mediaRepository.findById(request.mediaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found")));
        }
        if (!hasText(artwork.getImageName()) || !hasText(artwork.getFileName())
                || artwork.getWidth() == null || artwork.getHeight() == null || artwork.getYear() == null
                || artwork.getAuthor() == null || artwork.getMedia() == null) {
            throw badRequest("New artwork requires imageName, fileName, width, height, year, authorId and mediaId");
        }
        return mapper.toDetail(artworkRepository.save(artwork));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String text(String value, String field, int maxLength) {
        String result = value.strip();
        if (result.length() > maxLength) throw badRequest(field + " exceeds " + maxLength + " characters");
        return result;
    }

    private static <T extends Number> T positive(T value, String field) {
        if (value.longValue() <= 0) throw badRequest(field + " must be positive");
        return value;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
