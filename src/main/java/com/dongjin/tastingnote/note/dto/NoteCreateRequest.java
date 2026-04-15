package com.dongjin.tastingnote.note.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoteCreateRequest extends NoteBaseRequest {

    private Boolean isPublic = false;
}
