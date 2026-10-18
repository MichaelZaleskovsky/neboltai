package org.neboltai.artwork.entity;

import jakarta.persistence.*;
import lombok.*;

/** Kind of a medium on which an artwork is published (poster, magazine, postcard, book). */
@Entity
@Table(name = "media_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 256)
    private String type;

    @Column(length = 4000)
    private String description;
}
