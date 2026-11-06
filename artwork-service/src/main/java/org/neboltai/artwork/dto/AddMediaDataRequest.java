package org.neboltai.artwork.dto;

import java.util.UUID;

/** Null values and blank strings leave existing metadata unchanged. */
public record AddMediaDataRequest(
        UUID imageId, String imageName, String fileName, Long mediaId,
        Integer width, Integer height, Integer year, String description, Long authorId) {}
