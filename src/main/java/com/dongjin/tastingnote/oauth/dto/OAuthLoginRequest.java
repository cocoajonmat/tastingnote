package com.dongjin.tastingnote.oauth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginRequest(
        @NotBlank String code,
        @NotBlank String redirectUri
) {}