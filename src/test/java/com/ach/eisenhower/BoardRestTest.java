package com.ach.eisenhower;

import com.ach.eisenhower.services.BoardService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BoardRestTest extends EisenhowerBaseTest {
    @Autowired
    private BoardService boardService;

    @Test
    public void testCreateBoardAnonymousShouldFail() throws Exception {
        mvc.perform(post("/api/boards")
                        .content("""
                                { "title": "testBoard" }
                                """)
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateBoard() throws Exception {
        mvc.perform(post("/api/board/")
                        .content("""
                                { "title": "testBoard" }
                                """)
                        .cookie(new Cookie("token", userTestingUtils.getValidAccessToken()))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", anything()));
    }

    @Test
    public void testListBoards() throws Exception {
        var user = userTestingUtils.getValidUser();
        boardService.createBoard(user, "test board");
        mvc.perform(get("/api/board/list/")
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(user)))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title", is("test board")));
    }

    @Test
    public void testGetBoard() throws Exception {
        var user = userTestingUtils.getValidUser();
        var board = boardService.createBoard(user, "test board");
        mvc.perform(get("/api/board/" + board.getId())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(user)))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("title", is("test board")));
    }

    @Test
    public void testUserCannotAccessOthersBoards() throws Exception {
        var boardOwner = userTestingUtils.getValidUser();
        var board = boardService.createBoard(boardOwner, "test board");

        var otherUser = userTestingUtils.getValidUser();
        mvc.perform(get("/api/board/" + board.getId())
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(otherUser)))
                        .secure(true))
                .andExpect(status().isUnauthorized());

        var otherUserBoard = boardService.createBoard(otherUser, "test board 2");
        mvc.perform(get("/api/board/list/")
                        .cookie(new Cookie("token", userTestingUtils.getAccessTokenForUser(otherUser)))
                        .secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("test board 2")));
    }
}
