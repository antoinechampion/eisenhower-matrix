package com.ach.eisenhower.controllers;

import com.ach.eisenhower.controllers.dtos.NoteCreateDto;
import com.ach.eisenhower.controllers.dtos.NoteUpdateBatchDto;
import com.ach.eisenhower.controllers.dtos.NoteUpdateDto;
import com.ach.eisenhower.entities.NoteEntity;
import com.ach.eisenhower.services.NoteService;
import com.ach.eisenhower.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controller for notes management
 */
@RestController
@RequestMapping("/api/note")
public class NoteController {

    final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    /**
     * Creates a note in a given board
     */
    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteEntity createNote(@RequestBody NoteCreateDto dto) {
        var userId = UserService.getContextUserIdOrThrow(SecurityContextHolder.getContext());
        return noteService.createNote(
                userId,
                UUID.fromString(dto.getBoardId()),
                dto.getText(),
                dto.getUrgency(),
                dto.getImportance()
        );
    }

    /**
     * Get a note by ID
     */
    @GetMapping("/{id}")
    public NoteEntity getNote(@PathVariable String id) {
        var userId = UserService.getContextUserIdOrThrow(SecurityContextHolder.getContext());
        return noteService.getNote(userId, UUID.fromString(id));
    }

    /**
     * List all the notes in a single board
     */
    @GetMapping("/list/")
    public List<NoteEntity> getAllNotes(@RequestParam("boardId") UUID boardId) {
        var userId = UserService.getContextUserIdOrThrow(SecurityContextHolder.getContext());
        return noteService.listNotes(userId, boardId);
    }

    /**
     * Updates a single note
     */
    @PutMapping("/{id}")
    public NoteEntity updateNote(@PathVariable String id, @RequestBody NoteUpdateDto dto) {
        var userId = UserService.getContextUserIdOrThrow(SecurityContextHolder.getContext());
        return noteService.updateNote(
                userId,
                UUID.fromString(id),
                dto.getText(),
                dto.getImportance(),
                dto.getUrgency()
        );
    }

    /**
     * Deletes a single note
     */
    @DeleteMapping("/{id}")
    public void deleteNote(@PathVariable String id) {
        var userId = UserService.getContextUserIdOrThrow(SecurityContextHolder.getContext());
        noteService.deleteNote(userId, UUID.fromString(id));
    }

    /**
     * Perform a batch update of a list of notes. If one of the update fails, the successful updates will
     * not be rolled back
     */
    @PutMapping("/batch/update")
    public void updateBatch(@RequestBody ArrayList<NoteUpdateBatchDto> dto) {
        var userId = UserService.getContextUserIdOrThrow(SecurityContextHolder.getContext());
        HttpStatusCodeException exception = null;
        for (var dtoElement : dto) {
            try {
                noteService.updateNote(
                        userId,
                        UUID.fromString(dtoElement.getId()),
                        dtoElement.getText(),
                        dtoElement.getImportance(),
                        dtoElement.getUrgency()
                );
            } catch (HttpStatusCodeException e) {
                exception = e;
            }
        }

        if (exception != null) throw exception;
    }

    /**
     * Performs a batch delete of notes
     */
    @DeleteMapping("/batch/delete")
    public void deleteBatch(@RequestBody ArrayList<String> ids) {
        var userId = UserService.getContextUserIdOrThrow(SecurityContextHolder.getContext());
        var uuids = ids.stream().map(UUID::fromString).toList();
        for (var noteId : uuids) {
            noteService.deleteNote(userId, noteId);
        }
    }
}
