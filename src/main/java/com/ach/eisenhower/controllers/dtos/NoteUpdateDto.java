package com.ach.eisenhower.controllers.dtos;

import lombok.Getter;
import lombok.Setter;

public class NoteUpdateDto {
    @Getter
    @Setter
    private String text;
    @Getter
    @Setter
    private double urgency;
    @Getter
    @Setter
    private double importance;
}
