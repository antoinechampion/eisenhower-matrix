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
import static org.hamcrest.CoreMatchers.nullValue;
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

    @Test
    public void testCreateNoteWithFlag() throws Exception {
        var board = createUserWithBoard();
        var token = userTestingUtils.getAccessTokenForUser(board.getUser());

        // Create with QUICK_WIN flag
        var createQuickWinResult = mvc.perform(post("/api/note/")
                        .content("{ \"boardId\": \"" + board.getId() + "\", \"text\": \"quick win note\", \"importance\": 0.8, \"urgency\": 0.2, \"flag\": \"QUICK_WIN\"}")
                        .cookie(new Cookie("token", token))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", anything()))
                .andExpect(jsonPath("flag", is("QUICK_WIN")))
                .andExpect(jsonPath("importance", is(0.8)))
                .andExpect(jsonPath("urgency", is(0.2)))
                .andReturn();

        String quickWinId = com.jayway.jsonpath.JsonPath.read(createQuickWinResult.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/note/" + quickWinId)
                        .cookie(new Cookie("token", token))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("flag", is("QUICK_WIN")))
                .andExpect(jsonPath("importance", is(0.8)))
                .andExpect(jsonPath("urgency", is(0.2)));

        // Create with LONG_TERM flag
        var createLongTermResult = mvc.perform(post("/api/note/")
                        .content("{ \"boardId\": \"" + board.getId() + "\", \"text\": \"long term note\", \"importance\": 0.3, \"urgency\": 0.7, \"flag\": \"LONG_TERM\"}")
                        .cookie(new Cookie("token", token))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("flag", is("LONG_TERM")))
                .andExpect(jsonPath("importance", is(0.3)))
                .andExpect(jsonPath("urgency", is(0.7)))
                .andReturn();

        String longTermId = com.jayway.jsonpath.JsonPath.read(createLongTermResult.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/note/" + longTermId)
                        .cookie(new Cookie("token", token))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("flag", is("LONG_TERM")))
                .andExpect(jsonPath("importance", is(0.3)))
                .andExpect(jsonPath("urgency", is(0.7)));

        // Create without flag (null)
        var createNullResult = mvc.perform(post("/api/note/")
                        .content("{ \"boardId\": \"" + board.getId() + "\", \"text\": \"unflagged note\", \"importance\": 0.5, \"urgency\": 0.5}")
                        .cookie(new Cookie("token", token))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("flag", nullValue()))
                .andReturn();

        String nullFlagId = com.jayway.jsonpath.JsonPath.read(createNullResult.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/note/" + nullFlagId)
                        .cookie(new Cookie("token", token))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("flag", nullValue()));

        // Check list endpoint returns all notes with their respective flags
        mvc.perform(get("/api/note/list/?boardId=" + board.getId())
                        .cookie(new Cookie("token", token))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + quickWinId + "')].flag").value("QUICK_WIN"))
                .andExpect(jsonPath("$[?(@.id == '" + longTermId + "')].flag").value("LONG_TERM"))
                .andExpect(jsonPath("$[?(@.id == '" + nullFlagId + "')].flag").value(hasItem(nullValue())));
    }

    @Test
    public void testUserCannotCreateNoteWithInvalidFlag() throws Exception {
        var board = createUserWithBoard();
        mvc.perform(post("/api/note/")
                        .content("{ \"boardId\": \"" + board.getId() + "\", \"text\": \"test note\", \"importance\": 0.1, \"urgency\": 0.1, \"flag\": \"INVALID_FLAG\"}")
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateNoteFlag() throws Exception {
        var board = createUserWithBoard();
        var token = userTestingUtils.getAccessTokenForUser(board.getUser());
        var note = noteService.createNote(board.getUser().getId(), board.getId(), "initial note", 0.2, 0.2);

        // Update with QUICK_WIN flag
        mvc.perform(put("/api/note/" + note.getId())
                        .content("{ \"text\": \"updated text\", \"importance\": 0.5, \"urgency\": 0.5, \"flag\": \"QUICK_WIN\"}")
                        .cookie(new Cookie("token", token))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("flag", is("QUICK_WIN")));

        mvc.perform(get("/api/note/" + note.getId())
                        .cookie(new Cookie("token", token))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("flag", is("QUICK_WIN")));

        // Update with LONG_TERM flag
        mvc.perform(put("/api/note/" + note.getId())
                        .content("{ \"text\": \"updated text 2\", \"importance\": 0.6, \"urgency\": 0.6, \"flag\": \"LONG_TERM\"}")
                        .cookie(new Cookie("token", token))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("flag", is("LONG_TERM")));

        // Clear flag with null
        mvc.perform(put("/api/note/" + note.getId())
                        .content("{ \"text\": \"cleared flag text\", \"importance\": 0.6, \"urgency\": 0.6, \"flag\": null}")
                        .cookie(new Cookie("token", token))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("flag", nullValue()));

        mvc.perform(get("/api/note/" + note.getId())
                        .cookie(new Cookie("token", token))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("flag", nullValue()));
    }

    @Test
    public void testUserCannotUpdateNoteWithInvalidFlag() throws Exception {
        var board = createUserWithBoard();
        var note = noteService.createNote(board.getUser().getId(), board.getId(), "test", 0.1, 0.1);

        mvc.perform(put("/api/note/" + note.getId())
                        .content("{ \"text\": \"updated text\", \"importance\": 0.5, \"urgency\": 0.5, \"flag\": \"INVALID\"}")
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(board.getUser())))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateNotesInBatchWithFlags() throws Exception {
        var board = createUserWithBoard();
        var token = userTestingUtils.getAccessTokenForUser(board.getUser());

        var note1 = noteService.createNote(board.getUser().getId(), board.getId(), "note 1", 0.1, 0.1, null);
        var note2 = noteService.createNote(board.getUser().getId(), board.getId(), "note 2", 0.2, 0.2, "QUICK_WIN");
        var note3 = noteService.createNote(board.getUser().getId(), board.getId(), "note 3", 0.3, 0.3, "LONG_TERM");

        // Batch update: note1 -> LONG_TERM, note2 -> null (clear), note3 -> QUICK_WIN
        String batchJson = "["
                + "{ \"id\": \"" + note1.getId() + "\", \"text\": \"note 1 updated\", \"importance\": 0.1, \"urgency\": 0.1, \"flag\": \"LONG_TERM\" },"
                + "{ \"id\": \"" + note2.getId() + "\", \"text\": \"note 2 updated\", \"importance\": 0.2, \"urgency\": 0.2, \"flag\": null },"
                + "{ \"id\": \"" + note3.getId() + "\", \"text\": \"note 3 updated\", \"importance\": 0.3, \"urgency\": 0.3, \"flag\": \"QUICK_WIN\" }"
                + "]";

        mvc.perform(put("/api/note/batch/update")
                        .content(batchJson)
                        .cookie(new Cookie("token", token))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get("/api/note/" + note1.getId())
                        .cookie(new Cookie("token", token))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("flag", is("LONG_TERM")));

        mvc.perform(get("/api/note/" + note2.getId())
                        .cookie(new Cookie("token", token))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("flag", nullValue()));

        mvc.perform(get("/api/note/" + note3.getId())
                        .cookie(new Cookie("token", token))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("flag", is("QUICK_WIN")));
    }

    @Test
    public void testFiveArgUpdateNotePreservesFlag() {
        var board = createUserWithBoard();
        var note = noteService.createNote(board.getUser().getId(), board.getId(), "initial", 0.1, 0.1, "QUICK_WIN");
        org.junit.jupiter.api.Assertions.assertEquals("QUICK_WIN", note.getFlag());

        var updated = noteService.updateNote(board.getUser().getId(), note.getId(), "updated text", 0.4, 0.4);
        org.junit.jupiter.api.Assertions.assertEquals("QUICK_WIN", updated.getFlag());
        org.junit.jupiter.api.Assertions.assertEquals("updated text", updated.getText());

        var fetched = noteService.getNote(board.getUser().getId(), note.getId());
        org.junit.jupiter.api.Assertions.assertEquals("QUICK_WIN", fetched.getFlag());
    }

    @Test
    public void testUpdateNotesInBatchWithInvalidFlag() throws Exception {
        var board = createUserWithBoard();
        var token = userTestingUtils.getAccessTokenForUser(board.getUser());

        var note1 = noteService.createNote(board.getUser().getId(), board.getId(), "note 1", 0.1, 0.1, "QUICK_WIN");
        var note2 = noteService.createNote(board.getUser().getId(), board.getId(), "note 2", 0.2, 0.2, "LONG_TERM");

        // Batch update where note2 has INVALID_FLAG
        String batchJson = "["
                + "{ \"id\": \"" + note1.getId() + "\", \"text\": \"note 1 updated\", \"importance\": 0.5, \"urgency\": 0.5, \"flag\": null },"
                + "{ \"id\": \"" + note2.getId() + "\", \"text\": \"note 2 updated\", \"importance\": 0.6, \"urgency\": 0.6, \"flag\": \"INVALID_FLAG\" }"
                + "]";

        mvc.perform(put("/api/note/batch/update")
                        .content(batchJson)
                        .cookie(new Cookie("token", token))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Note 2 should remain untouched with its original text and flag
        mvc.perform(get("/api/note/" + note2.getId())
                        .cookie(new Cookie("token", token))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("text", is("note 2")))
                .andExpect(jsonPath("flag", is("LONG_TERM")));
    }

    private BoardEntity createUserWithBoard() {
        var user = userTestingUtils.getValidUser();
        return boardService.createBoard(user, RandomStringUtils.randomAlphabetic(8));
    }
}
