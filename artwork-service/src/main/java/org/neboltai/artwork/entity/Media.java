package org.neboltai.artwork.entity;

import jakarta.persistence.*;
import lombok.*;

/** A concrete medium (for example a particular magazine) that has a {@link MediaType}. */
@Entity
@Table(name = "media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 256)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private MediaType type;

    @Column(length = 4000)
    private String description;
}
