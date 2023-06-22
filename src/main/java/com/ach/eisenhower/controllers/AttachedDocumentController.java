package com.ach.eisenhower.controllers;

import com.ach.eisenhower.entities.AttachedDocumentEntity;
import com.ach.eisenhower.repositories.AttachedDocumentRepository;
import com.ach.eisenhower.repositories.NoteRepository;
import com.ach.eisenhower.services.AttachedDocumentService;
import com.ach.eisenhower.services.EisenhowerServiceException;
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
        boolean isGuest;
        var userId = UserService.getContextUserId(SecurityContextHolder.getContext());
        AttachedDocumentEntity doc;

        try {
            doc = attachedDocumentService.getAttachedDocument(userId, UUID.fromString(noteId));
            isGuest = false;
        } catch (EisenhowerServiceException e) {
            doc =  attachedDocumentService.getAttachedDocumentAsGuest(UUID.fromString(noteId));
            isGuest = true;
        }

        var resource = new ByteArrayResource(doc.getContent());
        var response = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("file").build().toString());

        if (isGuest) {
            response = response.header("X-Document-Guest", "true");
        }

        return response.body(resource);
    }

    /**
     * Enables link sharing for an attached document
     */
    @PostMapping("/{noteId}/share")
    public void enableSharing(@PathVariable String noteId) {
        var userId = UserService.getContextUserIdOrThrow(SecurityContextHolder.getContext());
        attachedDocumentService.enableSharing(userId, UUID.fromString(noteId));
    }
}
