package com.dongjin.tastingnote.oauth.dto;

import com.dongjin.tastingnote.user.entity.Provider;

public record OAuthUserInfo(
        String providerId,
        String email,
        String nickname,
        String profileImageUrl,
        Provider provider
) {}