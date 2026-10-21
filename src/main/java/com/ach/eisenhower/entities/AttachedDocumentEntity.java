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

    @Setter
    private byte[] content;

    // @Id is on a getter (property access), so mapping annotations must be on getters too
    @Column(nullable = false)
    @Lob
    public byte[] getContent() {
        return content;
    }

    @Column(nullable = false)
    @Getter @Setter
    private boolean sharingEnabled = false;

    private UUID _noteId;
    @Id
    public UUID getNoteId() {
        return _noteId;
    }

    public void setNoteId(UUID noteId) {
        _noteId = noteId;
    }
}
