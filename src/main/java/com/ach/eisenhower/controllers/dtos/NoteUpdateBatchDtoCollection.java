package com.ach.eisenhower.controllers.dtos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;

public class NoteUpdateBatchDtoCollection {
    private final ArrayList<NoteUpdateBatchDto> dtos;

    public NoteUpdateBatchDtoCollection() {
        this.dtos = new ArrayList<>();
    }

    public void add(NoteUpdateBatchDto noteDto) {
        dtos.add(noteDto);
    }

}
