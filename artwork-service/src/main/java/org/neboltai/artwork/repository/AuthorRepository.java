package org.neboltai.artwork.repository;

import org.neboltai.artwork.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {
}
