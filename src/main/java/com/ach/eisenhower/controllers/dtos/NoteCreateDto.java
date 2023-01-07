package com.ach.eisenhower.controllers.dtos;

import lombok.Getter;
import lombok.Setter;

public class NoteCreateDto {
    @Getter
    @Setter
    private String text;
    @Getter
    @Setter
    private double urgency;
    @Getter
    @Setter
    private double importance;
    @Getter
    @Setter
    @ValidateUUID
    private String boardId;
}