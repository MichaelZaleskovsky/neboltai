package org.neboltai.artwork.service;

import lombok.RequiredArgsConstructor;
import org.neboltai.artwork.dto.ArtworkDetailDto;
import org.neboltai.artwork.dto.PageResponse;
import org.neboltai.artwork.entity.Artwork;
import org.neboltai.artwork.entity.Author;
import org.neboltai.artwork.entity.Media;
import org.neboltai.artwork.repository.ArtworkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ArtworkSearchService {
    private final ArtworkRepository repository;

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    /**
     * Searches artworks; all filters are optional and combined with AND. Results are sorted by year, then id.
     *
     * @param authorId   id of the author
     * @param mediaType  media type (substring match, e.g. poster)
     * @param mediaName  media name (substring match)
     * @param yearFrom   minimum year, inclusive
     * @param yearTo     maximum year, inclusive
     * @param page       zero-based page number
     * @param size       page size
     * @return one page of artworks with paging information
     */
    public PageResponse<ArtworkDetailDto> search(
            Long authorId,
            String mediaType,
            String mediaName,
            Integer yearFrom,
            Integer yearTo,
            int page,
            int size) {

        Specification<Artwork> spec = Specification.allOf(
                ArtworkSpecifications.authorIdEquals(authorId),
                ArtworkSpecifications.mediaTypeContains(mediaType),
                ArtworkSpecifications.mediaNameContains(mediaName),
                ArtworkSpecifications.yearFrom(yearFrom),
                ArtworkSpecifications.yearTo(yearTo));

        Page<Artwork> result = repository.findAll(
                spec,
                PageRequest.of(page, size, Sort.by("year").ascending().and(Sort.by("id").ascending())));

        return new PageResponse<>(
                result.getContent().stream().map(this::toDetail).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private ArtworkDetailDto toDetail(Artwork a) {
        Author au = a.getAuthor();
        Media m = a.getMedia();
        String authorName = Stream.of(au.getFamilyName(), au.getName(), au.getSecondName())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));
        String previewUrl = publicBaseUrl.replaceAll("/$", "") + "/api/artworks/preview/" + a.getImageId();
        return new ArtworkDetailDto(
                a.getId(), a.getImageId(), a.getImageName(), a.getFileName(),
                a.getWidth(), a.getHeight(), a.getYear(), a.getDescription(),
                au.getId(), authorName,
                m.getId(), m.getName(), m.getType().getId(), m.getType().getType(),
                previewUrl);
    }
}
