package org.neboltai.artwork.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Artwork (image) metadata; {@code width} and {@code height} are the original master size in pixels. */
@Entity
@Table(name = "artwork")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artwork {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_id", nullable = false, unique = true)
    private UUID imageId;

    @Column(name = "image_name", nullable = false, length = 256)
    private String imageName;

    @Column(name = "file_name", nullable = false, length = 256)
    private String fileName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @Column(nullable = false)
    private Integer year;

    @Column(length = 4000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;
}
