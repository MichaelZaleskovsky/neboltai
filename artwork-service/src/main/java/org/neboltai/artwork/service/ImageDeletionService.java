package org.neboltai.artwork.service;

import lombok.RequiredArgsConstructor;
import org.neboltai.artwork.repository.ArtworkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageDeletionService {
    private final ImageStorageService imageStorageService;
    private final ArtworkRepository artworkRepository;

    @Transactional
    public void deleteImage(UUID imageId) {
        if (imageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageId is required");
        }
        // Remove objects first: storage failures must not erase the database metadata.
        // Missing objects and metadata are allowed so a partially completed deletion can be retried.
        imageStorageService.deleteImage(imageId);
        artworkRepository.deleteByImageId(imageId);
    }
}
