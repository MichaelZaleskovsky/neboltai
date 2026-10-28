package org.neboltai.artwork.service;

import org.neboltai.artwork.dto.ArtworkDetailDto;
import org.neboltai.artwork.entity.Artwork;
import org.neboltai.artwork.entity.Author;
import org.neboltai.artwork.entity.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ArtworkDtoMapper {
    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    public ArtworkDetailDto toDetail(Artwork a) {
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
