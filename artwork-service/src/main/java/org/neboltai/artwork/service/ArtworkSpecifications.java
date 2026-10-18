package org.neboltai.artwork.service;

import org.neboltai.artwork.entity.Artwork;
import org.springframework.data.jpa.domain.Specification;

public final class ArtworkSpecifications {
    private ArtworkSpecifications() {}

    public static Specification<Artwork> authorIdEquals(Long value) {
        return (root, query, cb) -> value == null
                ? cb.conjunction()
                : cb.equal(root.get("author").get("id"), value);
    }

    public static Specification<Artwork> mediaTypeContains(String value) {
        return (root, query, cb) -> value == null || value.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.join("media").join("type").get("type")), "%" + value.toLowerCase() + "%");
    }

    public static Specification<Artwork> mediaNameContains(String value) {
        return (root, query, cb) -> value == null || value.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.join("media").get("name")), "%" + value.toLowerCase() + "%");
    }

    public static Specification<Artwork> yearFrom(Integer value) {
        return (root, query, cb) -> value == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("year"), value);
    }

    public static Specification<Artwork> yearTo(Integer value) {
        return (root, query, cb) -> value == null
                ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("year"), value);
    }
}
