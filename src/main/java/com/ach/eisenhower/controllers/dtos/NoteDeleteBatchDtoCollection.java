package com.ach.eisenhower.controllers.dtos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.UUID;

public class NoteDeleteBatchDtoCollection {
    private final ArrayList<UUID> ids;

    public NoteDeleteBatchDtoCollection() {
        this.ids = new ArrayList<>();
    }

    public void add(UUID noteToDeleteId) {
        ids.add(noteToDeleteId);
    }
}
