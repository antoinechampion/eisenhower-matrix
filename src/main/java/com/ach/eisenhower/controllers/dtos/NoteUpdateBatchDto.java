package com.ach.eisenhower.controllers.dtos;

import lombok.Getter;
import lombok.Setter;

public class NoteUpdateBatchDto extends NoteUpdateDto {
    @Getter
    @Setter
    @ValidateUUID
    private String id;
}
