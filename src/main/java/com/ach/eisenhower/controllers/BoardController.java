package com.ach.eisenhower.controllers;

import com.ach.eisenhower.controllers.dtos.BoardCreateDto;
import com.ach.eisenhower.controllers.dtos.BoardUpdateDto;
import com.ach.eisenhower.entities.BoardEntity;
import com.ach.eisenhower.repositories.BoardRepository;
import com.ach.eisenhower.services.BoardService;
import com.ach.eisenhower.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for boards management
 */
@RestController
@RequestMapping("/api/board")
public class BoardController {
    final BoardRepository repository;
    final BoardService boardService;

    public BoardController(BoardRepository repository, BoardService boardService) {
        this.repository = repository;
        this.boardService = boardService;
    }

    /**
     * Creates a new board for the current user
     */
    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public BoardEntity createBoard(@RequestBody BoardCreateDto dto) {
        var user = UserService.getContextUser(SecurityContextHolder.getContext());
        return boardService.createBoard(user, dto.getTitle());
    }

    /**
     * Retrieves a board by its ID
     */
    @GetMapping("/{id}")
    public BoardEntity getBoard(@PathVariable String id) {
        var userId = UserService.getContextUserIdOrThrow(SecurityContextHolder.getContext());
        return boardService.getBoard(userId, UUID.fromString(id));
    }

    /**
     * Lists all the boards for the current user
     */
    @GetMapping("/list/")
    public List<BoardEntity> getAllBoardsForUser() {
        var user = UserService.getContextUser(SecurityContextHolder.getContext());
        return boardService.listBoardsForUser(user);
    }

    /**
     * Updates a board for a user
     */
    @PutMapping("/{id}")
    public void updateBoard(@PathVariable String id, @RequestBody BoardUpdateDto dto) {
        var userId = UserService.getContextUserIdOrThrow(SecurityContextHolder.getContext());
        boardService.updateBoard(userId, UUID.fromString(id), dto.getTitle());
    }

    /**
     * Deletes a board for a user
     */
    @DeleteMapping("/{id}")
    public void deleteBoard(@PathVariable String id) {
        var userId = UserService.getContextUserIdOrThrow(SecurityContextHolder.getContext());
        boardService.deleteBoard(userId, UUID.fromString(id));
    }
}
