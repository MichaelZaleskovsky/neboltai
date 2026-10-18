package org.neboltai.artwork.controller;

import lombok.RequiredArgsConstructor;
import org.neboltai.artwork.dto.AuthorDto;
import org.neboltai.artwork.dto.NamedItemDto;
import org.neboltai.artwork.dto.MediaTypeDto;
import org.neboltai.artwork.service.ReferenceDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReferenceDataController {
    private final ReferenceDataService service;

    @GetMapping("/authors")
    public List<AuthorDto> authors() {
        return service.authors();
    }

    @GetMapping("/medias")
    public List<NamedItemDto> medias() {
        return service.medias();
    }

    @GetMapping("/mediatypes")
    public List<MediaTypeDto> mediaTypes() {
        return service.mediaTypes();
    }
}
