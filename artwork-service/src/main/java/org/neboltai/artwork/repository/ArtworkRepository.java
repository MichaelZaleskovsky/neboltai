package org.neboltai.artwork.repository;

import org.neboltai.artwork.entity.Artwork;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ArtworkRepository extends JpaRepository<Artwork, Long>, JpaSpecificationExecutor<Artwork> {
    @EntityGraph(attributePaths = {"author", "media", "media.type"})
    Page<Artwork> findAll(Specification<Artwork> specification, Pageable pageable);
}
