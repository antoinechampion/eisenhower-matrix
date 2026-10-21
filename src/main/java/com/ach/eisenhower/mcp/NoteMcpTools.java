package com.ach.eisenhower.mcp;

import com.ach.eisenhower.entities.AttachedDocumentEntity;
import com.ach.eisenhower.entities.EisenhowerUserEntity;
import com.ach.eisenhower.entities.NoteEntity;
import com.ach.eisenhower.repositories.AttachedDocumentRepository;
import com.ach.eisenhower.repositories.EisenhowerUserRepository;
import com.ach.eisenhower.services.EisenhowerServiceException;
import com.ach.eisenhower.services.NoteService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Model Context Protocol (MCP) tool provider for Eisenhower matrix notes
 */
@Component
@RequiredArgsConstructor
public class NoteMcpTools {

    private final NoteService noteService;
    private final AttachedDocumentRepository attachedDocumentRepository;
    private final EisenhowerUserRepository userRepository;

    private static final Comparator<NoteSummary> NOTE_SUMMARY_COMPARATOR = Comparator
            .comparing((NoteSummary n) -> n.quadrant().ordinal())
            .thenComparing((NoteSummary n) -> Math.hypot(n.importance(), n.urgency()), Comparator.reverseOrder())
            .thenComparing(NoteSummary::importance, Comparator.reverseOrder())
            .thenComparing(NoteSummary::urgency, Comparator.reverseOrder())
            .thenComparing(NoteSummary::id);

    /**
     * Lists all notes across all boards for the authenticated user, categorized into
     * Eisenhower quadrants and sorted by priority distance.
     */
    @McpTool(
            name = "list_notes",
            description = "Lists all notes of the authenticated user sorted by quadrant priority (DO, SCHEDULE, DELEGATE, ELIMINATE) and distance from the high-priority origin.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    public List<NoteSummary> listNotes() {
        UUID userId = getAuthenticatedUserId();
        List<NoteEntity> notes = noteService.listUserNotes(userId);
        if (notes == null || notes.isEmpty()) {
            return List.of();
        }

        return notes.stream()
                .filter(note -> NoteService.userCanAccessNote(userId, note))
                .map(this::toNoteSummary)
                .sorted(NOTE_SUMMARY_COMPARATOR)
                .toList();
    }

    /**
     * Retrieves full details for a specific note including attached document HTML content if present.
     */
    @McpTool(
            name = "get_note",
            description = "Retrieves details for a specific note, including attached HTML document content if present.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    public NoteDetails getNote(
            @McpToolParam(description = "The unique identifier (UUID) of the note to retrieve")
            String id
    ) {
        if (id == null || id.isBlank()) {
            throw new EisenhowerServiceException(HttpStatus.BAD_REQUEST, "Note ID cannot be null or empty");
        }
        UUID noteId;
        try {
            noteId = UUID.fromString(id.trim());
        } catch (IllegalArgumentException e) {
            throw new EisenhowerServiceException(HttpStatus.BAD_REQUEST, "Invalid note ID format: " + id);
        }

        UUID userId = getAuthenticatedUserId();
        NoteEntity note = noteService.getNote(userId, noteId);

        AttachedDocumentEntity doc = attachedDocumentRepository.findByNoteId(note.getId());
        String documentContent = null;
        if (doc != null && doc.getContent() != null) {
            documentContent = new String(doc.getContent(), StandardCharsets.UTF_8);
        }

        double importance = 1.0 - note.getImportance();
        double urgency = 1.0 - note.getUrgency();
        Quadrant quadrant = calculateQuadrant(importance, urgency);
        String boardTitle = note.getBoard() != null ? note.getBoard().getTitle() : null;

        return new NoteDetails(
                note.getId(),
                note.getText(),
                quadrant,
                importance,
                urgency,
                boardTitle,
                documentContent
        );
    }

    /**
     * Converts a note entity to a note summary with inverted priority scores
     */
    private NoteSummary toNoteSummary(NoteEntity note) {
        double importance = 1.0 - note.getImportance();
        double urgency = 1.0 - note.getUrgency();
        Quadrant quadrant = calculateQuadrant(importance, urgency);
        String boardTitle = note.getBoard() != null ? note.getBoard().getTitle() : null;
        return new NoteSummary(
                note.getId(),
                note.getText(),
                quadrant,
                importance,
                urgency,
                boardTitle
        );
    }

    /**
     * Determines Eisenhower matrix quadrant based on inverted importance and urgency scores (> 0.5)
     */
    public static Quadrant calculateQuadrant(double importance, double urgency) {
        boolean isImportant = importance > 0.5;
        boolean isUrgent = urgency > 0.5;

        if (isImportant && isUrgent) {
            return Quadrant.DO;
        } else if (isImportant) {
            return Quadrant.SCHEDULE;
        } else if (isUrgent) {
            return Quadrant.DELEGATE;
        } else {
            return Quadrant.ELIMINATE;
        }
    }

    /**
     * Resolves the authenticated user ID from the security context
     */
    private UUID getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new EisenhowerServiceException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof Optional<?> opt && opt.isPresent()) {
            principal = opt.get();
        }
        if (principal instanceof EisenhowerUserEntity userEntity) {
            return userEntity.getId();
        }

        String resolvedUsername = null;
        if (principal instanceof Jwt jwt) {
            resolvedUsername = jwt.getSubject();
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            resolvedUsername = userDetails.getUsername();
        } else if (auth.getName() != null && !auth.getName().isBlank()) {
            resolvedUsername = auth.getName();
        }

        if (resolvedUsername == null || resolvedUsername.isBlank()) {
            throw new EisenhowerServiceException(HttpStatus.UNAUTHORIZED, "Cannot determine user identity from authentication token");
        }

        final String username = resolvedUsername;
        return userRepository.findByEmail(username)
                .map(EisenhowerUserEntity::getId)
                .orElseThrow(() -> new EisenhowerServiceException(HttpStatus.UNAUTHORIZED, "User not found: " + username));
    }
}
