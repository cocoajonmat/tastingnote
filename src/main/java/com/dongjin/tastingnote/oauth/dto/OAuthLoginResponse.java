package com.dongjin.tastingnote.oauth.dto;

public record OAuthLoginResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {}