package com.dongjin.tastingnote.oauth.client;

import com.dongjin.tastingnote.oauth.dto.OAuthUserInfo;
import com.dongjin.tastingnote.user.entity.Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class GoogleOAuthClient extends AbstractOAuthClient {

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    public GoogleOAuthClient(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public Provider provider() {
        return Provider.GOOGLE;
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String code, String redirectUri) {
        String accessToken = fetchAccessToken(
                "https://oauth2.googleapis.com/token", clientId, clientSecret, code, redirectUri);
        return parseUserInfo(accessToken);
    }

    private OAuthUserInfo parseUserInfo(String accessToken) {
        Map<?, ?> response = fetchUserInfoMap("https://www.googleapis.com/oauth2/v2/userinfo", accessToken);

        return new OAuthUserInfo(
                (String) response.get("id"),
                (String) response.get("email"),
                (String) response.get("name"),
                (String) response.get("picture"),
                Provider.GOOGLE
        );
    }
}