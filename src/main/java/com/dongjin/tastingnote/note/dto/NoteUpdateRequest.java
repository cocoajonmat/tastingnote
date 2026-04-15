package com.dongjin.tastingnote.note.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoteUpdateRequest extends NoteBaseRequest {

    @NotNull(message = "isPublic은 null일 수 없습니다")
    private Boolean isPublic;
}
