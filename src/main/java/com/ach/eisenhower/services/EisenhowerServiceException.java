package com.ach.eisenhower.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class EisenhowerServiceException extends ResponseStatusException {
    public EisenhowerServiceException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public EisenhowerServiceException(HttpStatus status) {
        super(status);
    }
    public EisenhowerServiceException(HttpStatus status, String message) {
        super(status, message);
    }
}
