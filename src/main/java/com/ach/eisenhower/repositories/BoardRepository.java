package com.ach.eisenhower.repositories;

import com.ach.eisenhower.entities.BoardEntity;
import com.ach.eisenhower.entities.EisenhowerUserEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

public interface BoardRepository extends Repository<BoardEntity, UUID> {
    BoardEntity findById(UUID id);

    List<BoardEntity> findAllByUser(EisenhowerUserEntity user);

    void save(BoardEntity board) throws NoSuchElementException;

    void deleteById(UUID id) throws NoSuchElementException;

    void deleteAll();
}
