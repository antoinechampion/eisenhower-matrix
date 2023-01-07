package com.ach.eisenhower.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import java.util.Set;
import java.util.UUID;

/**
 * An entire matrix that contains multiple notes
 */
@Entity
@Table(name = "boards")
@Builder
@NoArgsConstructor @AllArgsConstructor
public class BoardEntity {
    @ManyToOne(targetEntity = EisenhowerUserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @Getter @Setter
    private EisenhowerUserEntity user;

    @OneToMany(mappedBy = "id")
    private Set<NoteEntity> notes;

    @Getter @Setter
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Getter @Setter
    @Length(max = 256)
    @Column(nullable = false)
    private String title;
}
