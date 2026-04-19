package com.dongjin.tastingnote.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
        @Pattern(regexp = "^\\S+$", message = "닉네임에 공백을 포함할 수 없습니다.")
        String nickname
) {
}
