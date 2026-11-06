package org.neboltai.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AddMediaRequest(
        @Schema(example = "Photo Magazine", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 256) String name,
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") Long typeId,
        @Schema(example = "Monthly photography magazine", requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 4000) String description) {}
