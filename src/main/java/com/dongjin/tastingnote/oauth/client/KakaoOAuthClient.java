package com.dongjin.tastingnote.oauth.client;

import com.dongjin.tastingnote.oauth.dto.OAuthUserInfo;
import com.dongjin.tastingnote.user.entity.Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class KakaoOAuthClient implements OAuthClient {

    private final RestTemplate restTemplate;

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.client-secret}")
    private String clientSecret;

    public KakaoOAuthClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Provider provider() {
        return Provider.KAKAO;
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String code, String redirectUri) {
        String accessToken = getAccessToken(code, redirectUri);
        return getUserInfo(accessToken);
    }

    private String getAccessToken(String code, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        Map<?, ?> response = restTemplate.postForObject(
                "https://kauth.kakao.com/oauth/token",
                new HttpEntity<>(body, headers),
                Map.class
        );
        return (String) response.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        Map<?, ?> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        ).getBody();

        String providerId = String.valueOf(response.get("id"));
        Map<String, Object> kakaoAccount = (Map<String, Object>) response.get("kakao_account");
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
        Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
        String nickname = profile != null ? (String) profile.get("nickname") : null;
        String profileImageUrl = profile != null ? (String) profile.get("profile_image_url") : null;

        return new OAuthUserInfo(providerId, email, nickname, profileImageUrl, Provider.KAKAO);
    }
}