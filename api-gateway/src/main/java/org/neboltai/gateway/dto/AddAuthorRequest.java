package org.neboltai.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AddAuthorRequest(
        @Schema(example = "Vasiliev", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 256) String familyName,
        @Schema(example = "Vasia", requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 256) String name,
        @Schema(example = "Vasilich", requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 256) String secondName,
        @Schema(example = "Photographer", requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 4000) String description) {}
