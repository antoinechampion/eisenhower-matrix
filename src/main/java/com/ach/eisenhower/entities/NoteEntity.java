package com.ach.eisenhower.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import java.util.UUID;

import static java.lang.Math.abs;

/**
 * Single task within a matrix board
 */
@Entity
@Table(name = "notes")
@Builder
@NoArgsConstructor @AllArgsConstructor
public class NoteEntity {
    @Getter @Setter
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Getter @Setter
    @Column(nullable = false)
    @Length(max = 2048)
    private String text;
    @Getter
    @Column(nullable = false)
    private double urgency;
    public void setUrgency(double other) {
        if (abs(other) > 1.0) {
            throw new IllegalArgumentException(
                    "Argument was expected to be in the [-1, 1] range"
            );
        }
        this.urgency = other;
    }

    @Getter
    @Column(nullable = false)
    private double importance;
    public void setImportance(double other) {
        if (abs(other) > 1.0) {
            throw new IllegalArgumentException(
                    "Argument was expected to be in the [-1, 1] range"
            );
        }
        this.importance = other;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "board_id", nullable = false)
    @JsonIgnore
    @Getter @Setter
    private BoardEntity board;

    @OneToOne(targetEntity = AttachedDocumentEntity.class, cascade = CascadeType.REMOVE)
    @PrimaryKeyJoinColumn
    private AttachedDocumentEntity attachedDocument;
}
