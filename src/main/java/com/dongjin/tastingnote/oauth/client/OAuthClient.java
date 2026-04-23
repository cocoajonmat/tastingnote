package com.dongjin.tastingnote.oauth.client;

import com.dongjin.tastingnote.oauth.dto.OAuthUserInfo;
import com.dongjin.tastingnote.user.entity.Provider;

public interface OAuthClient {
    Provider provider();
    OAuthUserInfo fetchUserInfo(String code, String redirectUri);
}