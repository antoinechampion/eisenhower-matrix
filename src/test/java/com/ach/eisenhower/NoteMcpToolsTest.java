package com.ach.eisenhower;

import com.ach.eisenhower.entities.AttachedDocumentEntity;
import com.ach.eisenhower.entities.EisenhowerUserEntity;
import com.ach.eisenhower.entities.NoteEntity;
import com.ach.eisenhower.mcp.NoteDetails;
import com.ach.eisenhower.mcp.NoteMcpTools;
import com.ach.eisenhower.mcp.NoteSummary;
import com.ach.eisenhower.mcp.Quadrant;
import com.ach.eisenhower.repositories.AttachedDocumentRepository;
import com.ach.eisenhower.repositories.NoteRepository;
import com.ach.eisenhower.services.BoardService;
import com.ach.eisenhower.services.EisenhowerServiceException;
import com.ach.eisenhower.services.NoteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class NoteMcpToolsTest extends EisenhowerBaseTest {

    @Autowired
    private NoteMcpTools noteMcpTools;

    @Autowired
    private NoteService noteService;

    @Autowired
    private BoardService boardService;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private AttachedDocumentRepository attachedDocumentRepository;

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testReadOnlyToolsAdvertiseAccurateHints() throws NoSuchMethodException {
        assertReadOnlyToolHints(NoteMcpTools.class.getMethod("listNotes").getAnnotation(McpTool.class));
        assertReadOnlyToolHints(NoteMcpTools.class.getMethod("getNote", String.class).getAnnotation(McpTool.class));
    }

    private void assertReadOnlyToolHints(McpTool tool) {
        assertNotNull(tool);
        assertTrue(tool.annotations().readOnlyHint());
        assertFalse(tool.annotations().destructiveHint());
        assertTrue(tool.annotations().idempotentHint());
        assertFalse(tool.annotations().openWorldHint());
    }

    private void authenticateAs(EisenhowerUserEntity user) {
        Jwt jwt = new Jwt(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("sub", user.getEmail(), "aud", List.of("http://localhost/mcp"))
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                jwt, null, user.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authenticateWithPrincipal(EisenhowerUserEntity user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                Optional.of(user), null, user.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    public void testQuadrantCalculation() {
        // DO: importance > 0.5 & urgency > 0.5
        assertEquals(Quadrant.DO, NoteMcpTools.calculateQuadrant(0.9, 0.9));
        assertEquals(Quadrant.DO, NoteMcpTools.calculateQuadrant(0.51, 0.51));

        // SCHEDULE: importance > 0.5 & urgency <= 0.5
        assertEquals(Quadrant.SCHEDULE, NoteMcpTools.calculateQuadrant(0.9, 0.5));
        assertEquals(Quadrant.SCHEDULE, NoteMcpTools.calculateQuadrant(0.9, 0.1));

        // DELEGATE: importance <= 0.5 & urgency > 0.5
        assertEquals(Quadrant.DELEGATE, NoteMcpTools.calculateQuadrant(0.5, 0.9));
        assertEquals(Quadrant.DELEGATE, NoteMcpTools.calculateQuadrant(0.1, 0.9));

        // ELIMINATE: importance <= 0.5 & urgency <= 0.5
        assertEquals(Quadrant.ELIMINATE, NoteMcpTools.calculateQuadrant(0.5, 0.5));
        assertEquals(Quadrant.ELIMINATE, NoteMcpTools.calculateQuadrant(0.2, 0.2));
    }

    @Test
    public void testListNotesCoordinateInversionAndQuadrantSorting() {
        var user = userTestingUtils.getValidUser();
        var board = boardService.createBoard(user, "Main Board");

        // Raw coordinates in NoteEntity: 0.0 is high priority, 1.0 is low priority
        // Inverted score = 1.0 - raw

        // DO note: raw importance=0.1 (inv=0.9), raw urgency=0.1 (inv=0.9)
        var doNote = noteService.createNote(user.getId(), board.getId(), "Do Note", 0.1, 0.1);

        // SCHEDULE note: raw importance=0.1 (inv=0.9), raw urgency=0.9 (inv=0.1)
        var scheduleNote = noteService.createNote(user.getId(), board.getId(), "Schedule Note", 0.1, 0.9);

        // DELEGATE note: raw importance=0.9 (inv=0.1), raw urgency=0.1 (inv=0.9)
        var delegateNote = noteService.createNote(user.getId(), board.getId(), "Delegate Note", 0.9, 0.1);

        // ELIMINATE note: raw importance=0.9 (inv=0.1), raw urgency=0.9 (inv=0.1)
        var eliminateNote = noteService.createNote(user.getId(), board.getId(), "Eliminate Note", 0.9, 0.9);

        authenticateAs(user);

        List<NoteSummary> results = noteMcpTools.listNotes();
        assertEquals(4, results.size());

        // Quadrant order must be DO -> SCHEDULE -> DELEGATE -> ELIMINATE
        assertEquals(doNote.getId(), results.get(0).id());
        assertEquals(Quadrant.DO, results.get(0).quadrant());
        assertEquals("Do Note", results.get(0).text());
        assertEquals(0.9, results.get(0).importance(), 0.001);
        assertEquals(0.9, results.get(0).urgency(), 0.001);
        assertEquals("Main Board", results.get(0).board());

        assertEquals(scheduleNote.getId(), results.get(1).id());
        assertEquals(Quadrant.SCHEDULE, results.get(1).quadrant());
        assertEquals("Schedule Note", results.get(1).text());
        assertEquals(0.9, results.get(1).importance(), 0.001);
        assertEquals(0.1, results.get(1).urgency(), 0.001);

        assertEquals(delegateNote.getId(), results.get(2).id());
        assertEquals(Quadrant.DELEGATE, results.get(2).quadrant());
        assertEquals("Delegate Note", results.get(2).text());
        assertEquals(0.1, results.get(2).importance(), 0.001);
        assertEquals(0.9, results.get(2).urgency(), 0.001);

        assertEquals(eliminateNote.getId(), results.get(3).id());
        assertEquals(Quadrant.ELIMINATE, results.get(3).quadrant());
        assertEquals("Eliminate Note", results.get(3).text());
        assertEquals(0.1, results.get(3).importance(), 0.001);
        assertEquals(0.1, results.get(3).urgency(), 0.001);
    }

    @Test
    public void testListNotesEuclideanDistanceAndTieBreakersWithinQuadrant() {
        var user = userTestingUtils.getValidUser();
        var board = boardService.createBoard(user, "Priority Board");

        // Within DO quadrant:
        // Note 1: raw 0.0, 0.0 -> inv 1.0, 1.0 (hypot = 1.414)
        var highPriority = noteService.createNote(user.getId(), board.getId(), "Highest DO", 0.0, 0.0);

        // Note 2: raw 0.2, 0.2 -> inv 0.8, 0.8 (hypot = 1.131)
        var midPriority = noteService.createNote(user.getId(), board.getId(), "Mid DO", 0.2, 0.2);

        // Note 3: raw 0.2, 0.4 -> inv 0.8, 0.6 (hypot = 1.0, inv imp=0.8, inv urg=0.6)
        var tieA = noteService.createNote(user.getId(), board.getId(), "Tie A (Higher Imp)", 0.2, 0.4);

        // Note 4: raw 0.4, 0.2 -> inv 0.6, 0.8 (hypot = 1.0, inv imp=0.6, inv urg=0.8)
        var tieB = noteService.createNote(user.getId(), board.getId(), "Tie B (Higher Urg)", 0.4, 0.2);

        authenticateAs(user);

        List<NoteSummary> results = noteMcpTools.listNotes();
        assertEquals(4, results.size());

        assertEquals(highPriority.getId(), results.get(0).id());
        assertEquals(midPriority.getId(), results.get(1).id());
        // For tie breakers with equal hypot (1.0): higher importance (0.8 vs 0.6) comes first
        assertEquals(tieA.getId(), results.get(2).id());
        assertEquals(tieB.getId(), results.get(3).id());
    }

    @Test
    public void testListNotesAcrossMultipleBoardsForSameUser() {
        var user = userTestingUtils.getValidUser();
        var board1 = boardService.createBoard(user, "Work Board");
        var board2 = boardService.createBoard(user, "Personal Board");

        var note1 = noteService.createNote(user.getId(), board1.getId(), "Work Task", 0.1, 0.1);
        var note2 = noteService.createNote(user.getId(), board2.getId(), "Personal Task", 0.1, 0.1);

        authenticateAs(user);

        List<NoteSummary> results = noteMcpTools.listNotes();
        assertEquals(2, results.size());

        List<UUID> noteIds = results.stream().map(NoteSummary::id).toList();
        assertTrue(noteIds.contains(note1.getId()));
        assertTrue(noteIds.contains(note2.getId()));

        List<String> boardNames = results.stream().map(NoteSummary::board).toList();
        assertTrue(boardNames.contains("Work Board"));
        assertTrue(boardNames.contains("Personal Board"));
    }

    @Test
    public void testListNotesExcludesOtherUsersNotes() {
        var user1 = userTestingUtils.getValidUser();
        var user2 = userTestingUtils.getValidUser();

        var board1 = boardService.createBoard(user1, "User 1 Board");
        var board2 = boardService.createBoard(user2, "User 2 Board");

        noteService.createNote(user1.getId(), board1.getId(), "User 1 Secret", 0.1, 0.1);
        noteService.createNote(user2.getId(), board2.getId(), "User 2 Secret", 0.1, 0.1);

        authenticateAs(user1);

        List<NoteSummary> results = noteMcpTools.listNotes();
        assertEquals(1, results.size());
        assertEquals("User 1 Secret", results.get(0).text());
        assertEquals("User 1 Board", results.get(0).board());
    }

    @Test
    public void testGetNoteWithAttachedDocument() {
        var user = userTestingUtils.getValidUser();
        var board = boardService.createBoard(user, "Doc Board");
        var note = noteService.createNote(user.getId(), board.getId(), "Task with HTML", 0.2, 0.3);

        String htmlContent = "<p>Meeting notes &amp; action items: <strong>Ship MCP!</strong></p>";
        var doc = AttachedDocumentEntity.builder()
                .note(note)
                ._noteId(note.getId())
                .content(htmlContent.getBytes(StandardCharsets.UTF_8))
                .sharingEnabled(false)
                .build();
        attachedDocumentRepository.save(doc);

        authenticateAs(user);

        NoteDetails details = noteMcpTools.getNote(note.getId().toString());
        assertNotNull(details);
        assertEquals(note.getId(), details.id());
        assertEquals("Task with HTML", details.text());
        assertEquals(Quadrant.DO, details.quadrant());
        assertEquals(0.8, details.importance(), 0.001);
        assertEquals(0.7, details.urgency(), 0.001);
        assertEquals("Doc Board", details.board());
        assertEquals(htmlContent, details.document());
    }

    @Test
    public void testGetNoteWithoutAttachedDocumentReturnsNullDoc() {
        var user = userTestingUtils.getValidUser();
        var board = boardService.createBoard(user, "Empty Doc Board");
        var note = noteService.createNote(user.getId(), board.getId(), "No Document Task", 0.8, 0.8);

        authenticateAs(user);

        NoteDetails details = noteMcpTools.getNote(note.getId().toString());
        assertNotNull(details);
        assertEquals(note.getId(), details.id());
        assertEquals("No Document Task", details.text());
        assertEquals(Quadrant.ELIMINATE, details.quadrant());
        assertNull(details.document());
    }

    @Test
    public void testGetNoteThrowsUnauthorizedForOtherUsersNote() {
        var user1 = userTestingUtils.getValidUser();
        var user2 = userTestingUtils.getValidUser();

        var board1 = boardService.createBoard(user1, "User 1 Board");
        var note1 = noteService.createNote(user1.getId(), board1.getId(), "Private Note", 0.1, 0.1);

        authenticateAs(user2);

        EisenhowerServiceException exception = assertThrows(
                EisenhowerServiceException.class,
                () -> noteMcpTools.getNote(note1.getId().toString())
        );
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    public void testGetNoteThrowsNotFoundForNonExistentNote() {
        var user = userTestingUtils.getValidUser();
        authenticateAs(user);

        EisenhowerServiceException exception = assertThrows(
                EisenhowerServiceException.class,
                () -> noteMcpTools.getNote(UUID.randomUUID().toString())
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void testGetNoteThrowsBadRequestForMalformedOrEmptyId() {
        var user = userTestingUtils.getValidUser();
        authenticateAs(user);

        assertThrows(
                EisenhowerServiceException.class,
                () -> noteMcpTools.getNote("not-a-valid-uuid")
        );

        assertThrows(
                EisenhowerServiceException.class,
                () -> noteMcpTools.getNote("")
        );

        assertThrows(
                EisenhowerServiceException.class,
                () -> noteMcpTools.getNote(null)
        );
    }

    @Test
    public void testUnauthenticatedAccessThrowsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThrows(
                EisenhowerServiceException.class,
                () -> noteMcpTools.listNotes()
        );

        assertThrows(
                EisenhowerServiceException.class,
                () -> noteMcpTools.getNote(UUID.randomUUID().toString())
        );
    }

    @Test
    public void testAuthenticationWithDirectUserPrincipal() {
        var user = userTestingUtils.getValidUser();
        var board = boardService.createBoard(user, "Direct Auth Board");
        noteService.createNote(user.getId(), board.getId(), "Direct Note", 0.1, 0.1);

        authenticateWithPrincipal(user);

        List<NoteSummary> notes = noteMcpTools.listNotes();
        assertEquals(1, notes.size());
        assertEquals("Direct Note", notes.get(0).text());
    }

    @Test
    public void testNoteRepositoryFindAllByBoardUserIdDirectly() {
        var user1 = userTestingUtils.getValidUser();
        var user2 = userTestingUtils.getValidUser();

        var board1 = boardService.createBoard(user1, "Board 1");
        var board2 = boardService.createBoard(user1, "Board 2");
        var board3 = boardService.createBoard(user2, "Board 3");

        noteService.createNote(user1.getId(), board1.getId(), "U1 N1", 0.1, 0.1);
        noteService.createNote(user1.getId(), board2.getId(), "U1 N2", 0.2, 0.2);
        noteService.createNote(user2.getId(), board3.getId(), "U2 N1", 0.3, 0.3);

        List<NoteEntity> user1Notes = noteRepository.findAllByBoardUserId(user1.getId());
        assertEquals(2, user1Notes.size());

        List<NoteEntity> user2Notes = noteRepository.findAllByBoardUserId(user2.getId());
        assertEquals(1, user2Notes.size());

        List<NoteEntity> nonExistentUserNotes = noteRepository.findAllByBoardUserId(UUID.randomUUID());
        assertTrue(nonExistentUserNotes.isEmpty());
    }
}
