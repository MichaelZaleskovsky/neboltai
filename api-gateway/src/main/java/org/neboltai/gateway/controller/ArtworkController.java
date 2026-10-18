package org.neboltai.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/// Public artwork API; every call goes through the gateway to the private artwork-service.
@RestController
@RequestMapping("/api/artworks")
@Tag(name = "Artworks")
public class ArtworkController {

    private final WebClient artworkClient;

    public ArtworkController(WebClient.Builder builder,
                             @Value("${app.artwork-service-url}") String artworkServiceUrl) {
        this.artworkClient = builder.baseUrl(artworkServiceUrl).build();
    }

    /// Searches artworks through the private artwork-service; all filters are optional and combined with AND.
    ///
    /// The response is a page `{items, page, size, totalElements, totalPages}`;
    /// every item has the artwork metadata and a `previewUrl`.
    ///
    /// Example: `GET /api/artworks/search?authorId=2&yearFrom=2018&yearTo=2021&page=0&size=20`
    ///
    /// @param authorId  id of the author
    /// @param mediaType media type, substring match (e.g. `poster`)
    /// @param mediaName media name, substring match
    /// @param yearFrom  minimum year, inclusive
    /// @param yearTo    maximum year, inclusive
    /// @param page      zero-based page number
    /// @param size      page size (1..100)
    /// @return the JSON page proxied from artwork-service
    @Operation(summary = "Search artworks (paged)",
            description = "Response: {items, page, size, totalElements, totalPages}; each item has all metadata and previewUrl.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(type = "object")))
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> search(
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String mediaType,
            @RequestParam(required = false) String mediaName,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return artworkClient.get()
                .uri(uri -> uri.path("/api/artworks/search")
                        .queryParamIfPresent("authorId", Optional.ofNullable(authorId))
                        .queryParamIfPresent("mediaType", Optional.ofNullable(mediaType))
                        .queryParamIfPresent("mediaName", Optional.ofNullable(mediaName))
                        .queryParamIfPresent("yearFrom", Optional.ofNullable(yearFrom))
                        .queryParamIfPresent("yearTo", Optional.ofNullable(yearTo))
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntityFlux(DataBuffer.class);
    }

    /// Returns the preview JPEG of an artwork for use in `<img src>`.
    ///
    /// Example: `GET /api/artworks/preview/{imageId}`
    ///
    /// @param imageId identifier of the image
    /// @return the JPEG proxied from artwork-service, or 404 if there is no preview
    @Operation(summary = "Preview JPEG of an artwork")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE,
            schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/preview/{imageId}", produces = MediaType.IMAGE_JPEG_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> preview(@PathVariable UUID imageId) {
        return artworkClient.get()
                .uri("/api/artworks/preview/{imageId}", imageId)
                .accept(MediaType.IMAGE_JPEG)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntityFlux(DataBuffer.class);
    }
}
