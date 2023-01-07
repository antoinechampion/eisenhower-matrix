package com.ach.eisenhower.repositories;

import com.ach.eisenhower.entities.BoardEntity;
import com.ach.eisenhower.entities.NoteEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

public interface NoteRepository extends Repository<NoteEntity, UUID> {
    NoteEntity findById(UUID id) throws NoSuchElementException;

    List<NoteEntity> findAllByBoardId(UUID boardId);

    void save(NoteEntity note) throws NoSuchElementException;

    void saveAll(Iterable<NoteEntity> notes) throws NoSuchElementException;

    void deleteById(UUID id) throws NoSuchElementException;

    void deleteAll();
}
