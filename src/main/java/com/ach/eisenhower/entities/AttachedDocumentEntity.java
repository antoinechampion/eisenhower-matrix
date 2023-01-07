package com.ach.eisenhower.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A memo document which can be bound to a note
 */
@Entity
@Table(name = "attached_documents")
@Builder
@NoArgsConstructor @AllArgsConstructor
public class AttachedDocumentEntity {
    @OneToOne
    @MapsId
    @JoinColumn(name = "note_id")
    private NoteEntity note;

    @Column(nullable = false)
    @Lob
    @Getter @Setter
    private byte[] content;

    private UUID _noteId;
    @Id
    public UUID getNoteId() {
        return _noteId;
    }

    public void setNoteId(UUID noteId) {
        _noteId = noteId;
    }
}
