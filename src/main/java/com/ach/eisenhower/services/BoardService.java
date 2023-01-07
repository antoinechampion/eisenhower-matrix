package com.ach.eisenhower.services;

import com.ach.eisenhower.entities.BoardEntity;
import com.ach.eisenhower.entities.EisenhowerUserEntity;
import com.ach.eisenhower.repositories.BoardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class BoardService {
    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    public BoardEntity createBoard(EisenhowerUserEntity user, String title) {
        validateBoardFieldsOrThrow(title);
        var board = BoardEntity.builder().user(user).title(title).build();
        boardRepository.save(board);
        return board;
    }

    public BoardEntity getBoard(UUID userId, UUID boardId) {
        var board = boardRepository.findById(boardId);

        if (board == null) {
            throw new EisenhowerServiceException(HttpStatus.NOT_FOUND, "Board does not exist");
        }
        if (!userCanAccessBoard(userId, board)) {
            throw new EisenhowerServiceException(HttpStatus.UNAUTHORIZED, "User does not have access to this board");
        }

        return board;
    }

    public List<BoardEntity> listBoardsForUser(EisenhowerUserEntity user) {
        return boardRepository.findAllByUser(user);
    }

    public void updateBoard(UUID userId, UUID boardId, String title) {
        var board = getBoard(userId, boardId);
        validateBoardFieldsOrThrow(title);
        board.setTitle(title);
        boardRepository.save(board);
    }

    public void deleteBoard(UUID userId, UUID boardId) {
        var board = getBoard(userId, boardId);
        boardRepository.deleteById(board.getId());
    }

    public static boolean userCanAccessBoard(UUID userId, BoardEntity board) {
        return board.getUser().getId().equals(userId);
    }

    private void validateBoardFieldsOrThrow(String title) {
        if (title.length() > 256) {
            throw new EisenhowerServiceException("Note text is above allowed limit of 256 characters");
        }
    }
}
