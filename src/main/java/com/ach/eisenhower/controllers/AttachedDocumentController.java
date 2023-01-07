package com.ach.eisenhower.controllers;

import com.ach.eisenhower.repositories.AttachedDocumentRepository;
import com.ach.eisenhower.repositories.NoteRepository;
import com.ach.eisenhower.services.AttachedDocumentService;
import com.ach.eisenhower.services.UserService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

/**
 * Controller for Attached Documents
 */
@RestController
@RequestMapping("/api/attached-document")
public class AttachedDocumentController {
    final AttachedDocumentRepository repository;
    final NoteRepository notesRepository;
    final AttachedDocumentService attachedDocumentService;


    public AttachedDocumentController(AttachedDocumentRepository repository, NoteRepository notesRepository, AttachedDocumentService attachedDocumentService) {
        this.repository = repository;
        this.notesRepository = notesRepository;
        this.attachedDocumentService = attachedDocumentService;
    }

    /**
     * Creates or update an attached document for a given note, which has to be sent as a binary file in a multipart request.
     */
    @PostMapping("/{noteId}")
    public void upsertAttachedDocument(
            @RequestParam("file") MultipartFile file,
            @PathVariable String noteId) {
        var userId = UserService.getContextUserIdOrThrow(SecurityContextHolder.getContext());
        try {
            var content = file.getBytes();
            attachedDocumentService.upsertAttachedDocument(userId, UUID.fromString(noteId), content);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read document");
        }
    }

    /**
     * Retrieves the attached document for a note as a binary stream.
     */
    @GetMapping("/{noteId}")
    public ResponseEntity<Resource> getAttachedDocument(@PathVariable String noteId) {
        var userId = UserService.getContextUserIdOrThrow(SecurityContextHolder.getContext());
        var doc = attachedDocumentService.getAttachedDocument(userId, UUID.fromString(noteId));

        var resource = new ByteArrayResource(doc.getContent());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("file").build().toString())
                .body(resource);
    }
}
