package org.neboltai.artwork.controller;

import lombok.RequiredArgsConstructor;
import org.neboltai.artwork.dto.ArtworkDetailDto;
import org.neboltai.artwork.dto.PageResponse;
import org.neboltai.artwork.service.ArtworkSearchService;
import org.neboltai.artwork.service.PreviewService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/artworks")
@RequiredArgsConstructor
public class ArtworkController {
    private final ArtworkSearchService searchService;
    private final PreviewService previewService;

    /**
     * Paged artwork search; {@code page} is zero-based, {@code size} is limited to 1..100.
     * Each item contains metadata, original width/height in pixels and {@code previewUrl}.
     */
    @GetMapping("/search")
    public PageResponse<ArtworkDetailDto> search(
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String mediaType,
            @RequestParam(required = false) String mediaName,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return searchService.search(authorId, mediaType, mediaName, yearFrom, yearTo,
                Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    /** Returns the preview JPEG of an artwork (cached by the browser for one day) or 404 if there is none. */
    @GetMapping(value = "/preview/{imageId}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> preview(@PathVariable UUID imageId) {
        return previewService.findPreview(imageId)
                .map(bytes -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(bytes))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
