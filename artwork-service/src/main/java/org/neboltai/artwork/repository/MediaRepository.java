package org.neboltai.artwork.repository;

import org.neboltai.artwork.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.neboltai.artwork.dto.NamedItemDto;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
    @Query("select new org.neboltai.artwork.dto.NamedItemDto(m.id, m.name) from Media m order by m.name, m.id")
    List<NamedItemDto> findAllItems();
}
