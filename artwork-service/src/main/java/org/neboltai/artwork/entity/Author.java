package org.neboltai.artwork.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "author")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_name", nullable = false, length = 256)
    private String familyName;

    @Column(length = 256)
    private String name;

    @Column(name = "second_name", length = 256)
    private String secondName;

    @Column(length = 4000)
    private String description;
}
