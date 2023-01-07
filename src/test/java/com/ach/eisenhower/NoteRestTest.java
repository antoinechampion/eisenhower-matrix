package com.ach.eisenhower;

import com.ach.eisenhower.entities.BoardEntity;
import com.ach.eisenhower.services.BoardService;
import com.ach.eisenhower.services.NoteService;
import jakarta.servlet.http.Cookie;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NoteRestTest extends EisenhowerBaseTest {
    @Autowired
    private BoardService boardService;
    @Autowired
    private NoteService noteService;

    @Test
    public void testCreateNoteInBoard() throws Exception {
        var board = createUserWithBoard();
        mvc.perform(post("/api/note/")
                        .content("{ \"boardId\": \"" + board.getId() + "\", \"text\": \"test note\", \"importance\": 0.1, \"urgency\": 0.1}")
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", anything()));
    }

    @Test
    public void testUserCannotCreateNoteWithInvalidData() throws Exception {
        var board = createUserWithBoard();
        mvc.perform(post("/api/note/")
                        .content("{ \"boardId\": \"" + board.getId() + "\", \"text\": \"test note\", \"importance\": -0.1, \"urgency\": 0.5}")
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/note/")
                        .content("{ \"boardId\": \"" + board.getId() + "\", \"text\": \"test note\", \"importance\": 0.5, \"urgency\": 2.5}")
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUserCannotCreateNoteWithTooLongText() throws Exception {
        var board = createUserWithBoard();
        var longText = RandomStringUtils.randomAlphabetic(4096);
        mvc.perform(post("/api/note/")
                        .content("{ \"boardId\": \"" + board.getId() + "\", \"text\": \"" + longText + "\", \"importance\": 0.5, \"urgency\": 0.5}")
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testListNotesForBoard() throws Exception {
        var board = createUserWithBoard();
        var noteText = RandomStringUtils.random(25);
        noteService.createNote(board.getUser().getId(), board.getId(), noteText, 0.3, 0.4);
        mvc.perform(get("/api/note/list/?boardId=" + board.getId())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text", is(noteText)))
                .andExpect(jsonPath("$[0].importance", is(0.3)))
                .andExpect(jsonPath("$[0].urgency", is(0.4)));
    }

    @Test
    public void testGetNote() throws Exception {
        var board = createUserWithBoard();
        var noteText = RandomStringUtils.random(25);
        var note = noteService.createNote(board.getUser().getId(), board.getId(), noteText, 0.3, 0.4);
        mvc.perform(get("/api/note/" + note.getId())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("text", is(noteText)))
                .andExpect(jsonPath("importance", is(0.3)))
                .andExpect(jsonPath("urgency", is(0.4)));
    }

    @Test
    public void testUserCannotAccessOthersNotes() throws Exception {
        var board = createUserWithBoard();
        var note = noteService.createNote(board.getUser().getId(), board.getId(), "test", 0.1, 0.1);
        var boardWithOtherUser = createUserWithBoard();

        mvc.perform(get("/api/note/" + note.getId())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(boardWithOtherUser.getUser())))
                        .secure(true))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete("/api/note/" + note.getId())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(boardWithOtherUser.getUser())))
                        .secure(true))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteNote() throws Exception {
        var board = createUserWithBoard();
        var note = noteService.createNote(board.getUser().getId(), board.getId(), "test", 0.1, 0.1);

        mvc.perform(delete("/api/note/" + note.getId())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true))
                .andExpect(status().isOk());

        mvc.perform(get("/api/note/" + note.getId())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateNote() throws Exception {
        var board = createUserWithBoard();
        var note = noteService.createNote(board.getUser().getId(), board.getId(), "test", 0.1, 0.1);

        mvc.perform(put("/api/note/" + note.getId())
                        .content("{ \"boardId\": \"" + board.getId() + "\", \"text\": \"updated text\", \"importance\": 0.5, \"urgency\": 0.5}")
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get("/api/note/" + note.getId())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("text", is("updated text")))
                .andExpect(jsonPath("importance", is(0.5)))
                .andExpect(jsonPath("urgency", is(0.5)));
    }

    @Test
    public void testUpdateNotesInBatch() throws Exception {
        var board = createUserWithBoard();
        StringBuilder json = new StringBuilder("[");

        final var numNotesToCreate = 10;
        var resultMatchers = new ResultMatcher[numNotesToCreate];
        for (int i = 0; i < numNotesToCreate; i++) {
            var note = noteService.createNote(board.getUser().getId(), board.getId(), "test", 0.1, 0.1);
            note.setText(RandomStringUtils.randomAlphabetic(10));
            resultMatchers[i] = jsonPath("$[*].text", hasItem(note.getText()));
            json.append("{ \"id\": \"").append(note.getId()).append("\", \"text\": \"").append(note.getText()).append("\", \"importance\": 0.1, \"urgency\": 0.1 },");
        }
        json.replace(json.length() - 1, json.length(), "]");

        mvc.perform(put("/api/note/batch/update")
                        .content(json.toString())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get("/api/note/list/?boardId=" + board.getId())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpectAll(resultMatchers);
    }

    private BoardEntity createUserWithBoard() {
        var user = userTestingUtils.getValidUser();
        return boardService.createBoard(user, RandomStringUtils.randomAlphabetic(8));
    }
}
