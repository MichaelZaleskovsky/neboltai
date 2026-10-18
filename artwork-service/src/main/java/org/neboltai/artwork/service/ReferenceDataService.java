package org.neboltai.artwork.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.neboltai.artwork.dto.AuthorDto;
import org.neboltai.artwork.dto.NamedItemDto;
import org.neboltai.artwork.dto.MediaTypeDto;
import org.neboltai.artwork.entity.Author;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferenceDataService {
    private final EntityManager entityManager;

    public List<AuthorDto> authors() {
        return entityManager.createQuery("select a from Author a", Author.class)
                .getResultList().stream()
                .map(a -> new AuthorDto(a.getId(),
                        Stream.of(a.getFamilyName(), a.getName(), a.getSecondName())
                                .filter(part -> part != null && !part.isBlank())
                                .map(String::strip)
                                .collect(Collectors.joining(" "))))
                .sorted(Comparator.comparing(AuthorDto::author).thenComparing(AuthorDto::id))
                .toList();
    }

    public List<NamedItemDto> medias() {
        return entityManager.createQuery("""
                select new org.neboltai.artwork.dto.NamedItemDto(m.id, m.name)
                from Media m order by m.name, m.id
                """, NamedItemDto.class).getResultList();
    }

    public List<MediaTypeDto> mediaTypes() {
        return entityManager.createQuery("""
                select new org.neboltai.artwork.dto.MediaTypeDto(t.id, t.type)
                from MediaType t order by t.type, t.id
                """, MediaTypeDto.class).getResultList();
    }
}

