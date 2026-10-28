package org.neboltai.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.neboltai.gateway.dto.AddAuthorRequest;
import org.neboltai.gateway.dto.AddMediaRequest;
import org.neboltai.gateway.dto.AddMediaTypeRequest;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@Tag(name = "Reference data")
public class ReferenceDataController {
    private final WebClient artworkClient;

    public ReferenceDataController(WebClient.Builder builder,
                                   @Value("${app.artwork-service-url}") String artworkServiceUrl) {
        this.artworkClient = builder.baseUrl(artworkServiceUrl).build();
    }

    @Operation(summary = "List all authors", description = "Array of {id, author}, sorted by full name.")
    @GetMapping(value = "/authors", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> authors() {
        return proxy("/api/authors");
    }

    @Operation(summary = "List all media", description = "Array of {id, name}, sorted by name.")
    @GetMapping(value = "/medias", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> medias() {
        return proxy("/api/medias");
    }

    @Operation(summary = "List all media types", description = "Array of {id, type}, sorted by type.")
    @GetMapping(value = "/mediatypes", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> mediaTypes() {
        return proxy("/api/mediatypes");
    }

    private Mono<ResponseEntity<Flux<DataBuffer>>> proxy(String path) {
        return artworkClient.get().uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntityFlux(DataBuffer.class);
    }

    @Operation(summary = "addAuthor", description = "Create from {familyName, name?, secondName?, description?}; returns the created item with its ID.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Missing or invalid required fields")
    @PostMapping(value = "/addAuthor", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> addAuthor(@RequestBody AddAuthorRequest request) {
        return proxyPost("/api/addAuthor", request);
    }

    @Operation(summary = "addMedia", description = "Create from {name, typeId, description?}; returns the created item with its ID.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Missing or invalid required fields")
    @PostMapping(value = "/addMedia", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> addMedia(@RequestBody AddMediaRequest request) {
        return proxyPost("/api/addMedia", request);
    }

    @Operation(summary = "addMediaType", description = "Create from {type, description?}; returns the created item with its ID.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Missing or invalid required fields")
    @PostMapping(value = "/addMediaType", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Flux<DataBuffer>>> addMediaType(@RequestBody AddMediaTypeRequest request) {
        return proxyPost("/api/addMediaType", request);
    }

    private Mono<ResponseEntity<Flux<DataBuffer>>> proxyPost(String path, Object request) {
        return artworkClient.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntityFlux(DataBuffer.class);
    }
}
