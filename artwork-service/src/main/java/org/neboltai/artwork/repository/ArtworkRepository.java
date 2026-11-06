package org.neboltai.artwork.repository;

import org.neboltai.artwork.entity.Artwork;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ArtworkRepository extends JpaRepository<Artwork, Long>, JpaSpecificationExecutor<Artwork> {
    Optional<Artwork> findByImageId(UUID imageId);

    long deleteByImageId(UUID imageId);

    @EntityGraph(attributePaths = {"author", "media", "media.type"})
    Page<Artwork> findAll(Specification<Artwork> specification, Pageable pageable);
}
