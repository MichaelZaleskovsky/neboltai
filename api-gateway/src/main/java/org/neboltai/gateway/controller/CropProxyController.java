package org.neboltai.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Images")
public class CropProxyController {

    private final WebClient cantaloupeClient;

    public CropProxyController(@Qualifier("cantaloupeClient") WebClient cantaloupeClient) {
        this.cantaloupeClient = cantaloupeClient;
    }

    /// Crop endpoint. The original file never leaves the private MinIO/Cantaloupe network.
    ///
    /// Example: `GET /api/images/7d1e0c10-0004-4a00-8000-000000000004/crop?x=100&y=100&width=1000&height=800&targetWidth=600`
    ///
    /// @param identifier  image id (the artwork `imageId`)
    /// @param x           left edge of the crop in original pixels
    /// @param y           top edge of the crop in original pixels
    /// @param width       crop width in original pixels
    /// @param height      crop height in original pixels
    /// @param targetWidth width of the returned JPEG in pixels
    /// @return the cropped JPEG proxied from Cantaloupe
    @Operation(summary = "Crop of an image as JPEG", description = "x/y/width/height are in original pixels; identifier is the artwork imageId.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE,
            schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/{identifier}/crop", produces = MediaType.IMAGE_JPEG_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> crop(
            @PathVariable String identifier,
            @RequestParam int x,
            @RequestParam int y,
            @RequestParam int width,
            @RequestParam int height,
            @RequestParam(defaultValue = "800") int targetWidth) {

        validate(x, y, width, height, targetWidth);
        String iiifPath = "/iiif/3/%s/%d,%d,%d,%d/%d,/0/default.jpg"
                .formatted(identifier, x, y, width, height, targetWidth);

        return cantaloupeClient.get()
                .uri(iiifPath)
                .accept(MediaType.asMediaType(MimeTypeUtils.IMAGE_JPEG))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntityFlux(DataBuffer.class);
    }

    private static void validate(int x, int y, int width, int height, int targetWidth) {
        if (x < 0 || y < 0 || width <= 0 || height <= 0 || targetWidth <= 0) {
            throw new IllegalArgumentException("x/y must be >= 0; width/height/targetWidth must be > 0");
        }
    }
}
