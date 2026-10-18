package org.neboltai.artwork.dto;

import java.util.UUID;

/**
 * Artwork metadata returned by the search endpoint.
 *
 * @param imageId     identifier of the image (MinIO key and IIIF identifier)
 * @param fileName    original file name, informational only
 * @param width       width of the original image in pixels
 * @param height      height of the original image in pixels
 * @param year        year of creation
 * @param authorId    id of the author
 * @param authorName  full author name: family name, name and second name (missing parts are skipped)
 * @param mediaId     id of the medium
 * @param mediaName   name of the medium
 * @param mediaTypeId id of the medium type
 * @param mediaType   medium type (poster, magazine, postcard, book)
 * @param previewUrl  URL of the preview JPEG (use it as {@code <img src>})
 */
public record ArtworkDetailDto(
        Long id,
        UUID imageId,
        String imageName,
        String fileName,
        Integer width,
        Integer height,
        Integer year,
        String description,
        Long authorId,
        String authorName,
        Long mediaId,
        String mediaName,
        Long mediaTypeId,
        String mediaType,
        String previewUrl) {
}
