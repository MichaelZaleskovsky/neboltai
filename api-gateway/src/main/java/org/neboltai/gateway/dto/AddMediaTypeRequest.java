package org.neboltai.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AddMediaTypeRequest(
        @Schema(example = "magazine", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 256) String type,
        @Schema(example = "Periodical publication", requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 4000) String description) {}
