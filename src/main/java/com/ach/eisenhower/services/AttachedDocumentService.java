package com.ach.eisenhower.services;

import com.ach.eisenhower.entities.AttachedDocumentEntity;
import com.ach.eisenhower.repositories.AttachedDocumentRepository;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AttachedDocumentService {
    final AttachedDocumentRepository attachedDocumentRepository;
    final NoteService noteService;

    @Getter
    final int maxDocumentSizeInBytes = 1000 * 1000 * 5;

    public AttachedDocumentService(AttachedDocumentRepository attachedDocumentRepository, NoteService noteService) {
        this.attachedDocumentRepository = attachedDocumentRepository;
        this.noteService = noteService;
    }

    public AttachedDocumentEntity getAttachedDocument(UUID userId, UUID noteId) {
        var note = noteService.getNote(userId, noteId);
        var doc = attachedDocumentRepository.findByNoteId(note.getId());
        if (doc == null) {
            throw new EisenhowerServiceException(HttpStatus.NOT_FOUND, "This note does not have any attached documents");
        }

        return doc;
    }

    public void upsertAttachedDocument(UUID userId, UUID noteId, byte[] content) {
        var note = noteService.getNote(userId, noteId);

        if (content.length > getMaxDocumentSizeInBytes()) {
            throw new EisenhowerServiceException(HttpStatus.PAYLOAD_TOO_LARGE);
        }

        var document = new AttachedDocumentEntity();
        document.setNoteId(note.getId());
        document.setContent(content);
        attachedDocumentRepository.save(document);
    }
}
