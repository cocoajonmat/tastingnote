package com.dongjin.tastingnote.feedback.dto;

import com.dongjin.tastingnote.feedback.entity.FeedbackCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FeedbackRequest {

    @NotNull(message = "카테고리는 필수입니다 (BUG, FEEDBACK, SUGGESTION)")
    private FeedbackCategory category;

    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 2000, message = "내용은 2000자 이하여야 합니다")
    private String content;

    private String appVersion;
}
