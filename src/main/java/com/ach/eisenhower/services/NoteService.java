package com.ach.eisenhower.services;

import com.ach.eisenhower.entities.NoteEntity;
import com.ach.eisenhower.repositories.NoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class NoteService {
    final NoteRepository noteRepository;

    final BoardService boardService;

    public NoteService(NoteRepository noteRepository, BoardService boardService) {
        this.noteRepository = noteRepository;
        this.boardService = boardService;
    }

    public NoteEntity createNote(UUID userId, UUID boardId, String text, double importance, double urgency) {
        var board = boardService.getBoard(userId, boardId);
        validateNoteFieldsOrThrow(text, importance, urgency);

        var note = NoteEntity.builder()
                .text(text).importance(importance).urgency(urgency).board(board)
                .build();
        noteRepository.save(note);
        return note;
    }

    public NoteEntity getNote(UUID userId, UUID noteId) {
        var note = noteRepository.findById(noteId);
        if (note == null) {
            throw new EisenhowerServiceException(HttpStatus.NOT_FOUND, "Note does not exist");
        }
        if (!userCanAccessNote(userId, note)) {
            throw new EisenhowerServiceException(HttpStatus.UNAUTHORIZED, "User does not have access to this note");
        }

        return note;
    }

    public List<NoteEntity> listNotes(UUID userId, UUID boardId) {
        var board = boardService.getBoard(userId, boardId);
        return noteRepository.findAllByBoardId(boardId);
    }

    public NoteEntity updateNote(UUID userId, UUID noteId, String text, double importance, double urgency) {
        var note = getNote(userId, noteId);
        if (!userCanAccessNote(userId, note)) {
            throw new EisenhowerServiceException(HttpStatus.UNAUTHORIZED, "The user does not have the right to list the notes of this board");
        }
        validateNoteFieldsOrThrow(text, importance, urgency);
        note.setText(text);
        note.setImportance(importance);
        note.setUrgency(urgency);
        noteRepository.save(note);
        return note;
    }

    public void deleteNote(UUID userId, UUID noteId) {
        var note = getNote(userId, noteId);
        if (!userCanAccessNote(userId, note)) {
            throw new EisenhowerServiceException(HttpStatus.UNAUTHORIZED, "The user does not have the right to delete this note");
        }
        noteRepository.deleteById(noteId);
    }

    public static boolean userCanAccessNote(UUID userId, NoteEntity note) {
        return note.getBoard().getUser().getId().equals(userId);
    }

    private void validateNoteFieldsOrThrow(String text, double importance, double urgency) {
        if (urgency > 1.0 || urgency < 0.0) {
            throw new EisenhowerServiceException("Urgency is out of bounds [0, 1]");
        }
        if (importance > 1.0 || importance < 0.0) {
            throw new EisenhowerServiceException("Importance is out of bounds [0, 1]");
        }
        if (text.length() > 2048) {
            throw new EisenhowerServiceException("Note text is above allowed limit of 2048 characters");
        }
    }
}
