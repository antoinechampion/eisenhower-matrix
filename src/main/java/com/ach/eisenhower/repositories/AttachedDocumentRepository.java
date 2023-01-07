package com.ach.eisenhower.repositories;

import com.ach.eisenhower.entities.AttachedDocumentEntity;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface AttachedDocumentRepository
        extends Repository<AttachedDocumentEntity, UUID> {

    AttachedDocumentEntity findByNoteId(UUID noteId);

    void save(AttachedDocumentEntity document);

}
