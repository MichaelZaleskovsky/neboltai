package org.neboltai.artwork.dto;

import java.util.UUID;

public record AddImageResponse(UUID imageId, String fileName, int width, int height,
                               int previewWidth, int previewHeight, String previewUrl) {}
