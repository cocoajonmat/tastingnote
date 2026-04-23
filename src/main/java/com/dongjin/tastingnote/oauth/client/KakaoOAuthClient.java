package com.dongjin.tastingnote.oauth.client;

import com.dongjin.tastingnote.oauth.dto.OAuthUserInfo;
import com.dongjin.tastingnote.user.entity.Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class KakaoOAuthClient extends AbstractOAuthClient {

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.client-secret}")
    private String clientSecret;

    public KakaoOAuthClient(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public Provider provider() {
        return Provider.KAKAO;
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String code, String redirectUri) {
        String accessToken = fetchAccessToken(
                "https://kauth.kakao.com/oauth/token", clientId, clientSecret, code, redirectUri);
        return parseUserInfo(accessToken);
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo parseUserInfo(String accessToken) {
        Map<?, ?> response = fetchUserInfoMap("https://kapi.kakao.com/v2/user/me", accessToken);

        String providerId = String.valueOf(response.get("id"));
        Map<String, Object> kakaoAccount = (Map<String, Object>) response.get("kakao_account");
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
        Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
        String nickname = profile != null ? (String) profile.get("nickname") : null;
        String profileImageUrl = profile != null ? (String) profile.get("profile_image_url") : null;

        return new OAuthUserInfo(providerId, email, nickname, profileImageUrl, Provider.KAKAO);
    }
}