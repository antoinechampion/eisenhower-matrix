package com.ach.eisenhower;

import com.ach.eisenhower.repositories.BoardRepository;
import com.ach.eisenhower.repositories.EisenhowerUserRepository;
import com.ach.eisenhower.repositories.NoteRepository;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class TestCleaningUtil {
    private final EisenhowerUserRepository userRepository;
    private final BoardRepository boardRepository;
    private final NoteRepository noteRepository;

    public TestCleaningUtil(EisenhowerUserRepository userRepository, BoardRepository boardRepository, NoteRepository noteRepository) {
        this.userRepository = userRepository;
        this.boardRepository = boardRepository;
        this.noteRepository = noteRepository;
    }

    public void cleanUsers() {
        userRepository.deleteAll();
    }
    public void cleanBoards() {
        boardRepository.deleteAll();
    }
    public void cleanNotes() {
        noteRepository.deleteAll();
    }
}
