package org.neboltai.artwork.repository;

import org.neboltai.artwork.entity.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.neboltai.artwork.dto.MediaTypeDto;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MediaTypeRepository extends JpaRepository<MediaType, Long> {
    @Query("select new org.neboltai.artwork.dto.MediaTypeDto(t.id, t.type) from MediaType t order by t.type, t.id")
    List<MediaTypeDto> findAllItems();
}
