package com.ach.eisenhower;

import com.ach.eisenhower.entities.NoteEntity;
import com.ach.eisenhower.services.AttachedDocumentService;
import com.ach.eisenhower.services.BoardService;
import com.ach.eisenhower.services.NoteService;
import jakarta.servlet.http.Cookie;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AttachedDocumentTest extends EisenhowerBaseTest {

    @Autowired
    private BoardService boardService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private AttachedDocumentService attachedDocumentService;

    @Test
    public void testGetEmptyAttachedDocument() throws Exception {
        var note = createUserWithBoardAndNote();
        mvc.perform(get("/api/attached-document/" + note.getId())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(note.getBoard().getUser())))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateAttachedDocument() throws Exception {
        var note = createUserWithBoardAndNote();
        var content = new byte[200];
        new Random().nextBytes(content);

        mvc.perform(multipart("/api/attached-document/" + note.getId())
                        .file(new MockMultipartFile("file", "file", "text/plain", content))
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(note.getBoard().getUser()))))
                .andExpect(status().isOk());

        var document = attachedDocumentService.getAttachedDocument(note.getBoard().getUser().getId(), note.getId());
        assertArrayEquals(document.getContent(), content);
    }

    @Test
    public void testCreateAttachedDocumentWithGuestAccess() throws Exception {
        var note = createUserWithBoardAndNote();
        var author = note.getBoard().getUser();
        var content = new byte[200];
        new Random().nextBytes(content);
        var newUser = userTestingUtils.createTestUser("test-user-2@foobar.com", "Azerty123");

        mvc.perform(multipart("/api/attached-document/" + note.getId())
                        .file(new MockMultipartFile("file", "file", "text/plain", content))
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(note.getBoard().getUser()))))
                .andExpect(status().isOk());

        // Access as guest should fail when sharing is not enabled
        mvc.perform(get("/api/attached-document/" + note.getId()))
                .andExpect(status().isUnauthorized());

        // Access as another user should fail when sharing is not enabled
        mvc.perform(get("/api/attached-document/" + note.getId())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(newUser))))
                .andExpect(status().isUnauthorized());

        // Enable sharing for the document
        mvc.perform(post("/api/attached-document/" + note.getId() + "/share")
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(author)))
                        .secure(true))
                .andExpect(status().isOk());

        // Access as guest
        mvc.perform(get("/api/attached-document/" + note.getId()))
                .andExpect(status().isOk());

        // Access as another user
        mvc.perform(get("/api/attached-document/" + note.getId())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(newUser))))
                .andExpect(status().isOk());
    }

    @Test
    public void testCreateTooLargeDocument() throws Exception {
        var note = createUserWithBoardAndNote();
        var content = new byte[attachedDocumentService.getMaxDocumentSizeInBytes() + 1];
        new Random().nextBytes(content);

        mvc.perform(multipart("/api/attached-document/" + note.getId())
                        .file(new MockMultipartFile("file", "file", "text/plain", content))
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(note.getBoard().getUser()))))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    public void testUpdateAttachedDocument() throws Exception {
        var note = createUserWithBoardAndNote();
        var content = new byte[200];
        new Random().nextBytes(content);
        attachedDocumentService.upsertAttachedDocument(note.getBoard().getUser().getId(), note.getId(), content);

        new Random().nextBytes(content);
        mvc.perform(multipart("/api/attached-document/" + note.getId())
                        .file(new MockMultipartFile("file", "file", "text/plain", content))
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(note.getBoard().getUser()))))
                .andExpect(status().isOk());

        var updatedDocument = attachedDocumentService.getAttachedDocument(note.getBoard().getUser().getId(), note.getId());
        assertArrayEquals(updatedDocument.getContent(), content);
    }

    @Test
    public void testGetAttachedDocument() throws Exception {
        var note = createUserWithBoardAndNote();
        var content = new byte[200];
        new Random().nextBytes(content);
        attachedDocumentService.upsertAttachedDocument(note.getBoard().getUser().getId(), note.getId(), content);

        new Random().nextBytes(content);
        mvc.perform(multipart("/api/attached-document/" + note.getId())
                        .file(new MockMultipartFile("file", "file", "text/plain", content))
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(note.getBoard().getUser()))))
                .andExpect(status().isOk());

        var document = attachedDocumentService.getAttachedDocument(note.getBoard().getUser().getId(), note.getId());
        assertArrayEquals(document.getContent(), content);
    }

    private NoteEntity createUserWithBoardAndNote() {
        var user = userTestingUtils.getValidUser();
        var board = boardService.createBoard(user, RandomStringUtils.randomAlphabetic(8));
        return noteService.createNote(user.getId(), board.getId(), RandomStringUtils.randomAlphabetic(20), 0.5, 0.5);
    }
}
