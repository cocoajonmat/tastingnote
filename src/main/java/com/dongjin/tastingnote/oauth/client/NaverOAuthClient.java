package com.dongjin.tastingnote.oauth.client;

import com.dongjin.tastingnote.oauth.dto.OAuthUserInfo;
import com.dongjin.tastingnote.user.entity.Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class NaverOAuthClient extends AbstractOAuthClient {

    @Value("${oauth.naver.client-id}")
    private String clientId;

    @Value("${oauth.naver.client-secret}")
    private String clientSecret;

    public NaverOAuthClient(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public Provider provider() {
        return Provider.NAVER;
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String code, String redirectUri) {
        String accessToken = fetchAccessToken(
                "https://nid.naver.com/oauth2.0/token", clientId, clientSecret, code, redirectUri);
        return parseUserInfo(accessToken);
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo parseUserInfo(String accessToken) {
        Map<?, ?> response = fetchUserInfoMap("https://openapi.naver.com/v1/nid/me", accessToken);

        Map<String, Object> naverResponse = (Map<String, Object>) response.get("response");
        return new OAuthUserInfo(
                (String) naverResponse.get("id"),
                (String) naverResponse.get("email"),
                (String) naverResponse.get("nickname"),
                (String) naverResponse.get("profile_image"),
                Provider.NAVER
        );
    }
}