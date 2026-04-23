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
public class NaverOAuthClient implements OAuthClient {

    private final RestTemplate restTemplate;

    @Value("${oauth.naver.client-id}")
    private String clientId;

    @Value("${oauth.naver.client-secret}")
    private String clientSecret;

    public NaverOAuthClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Provider provider() {
        return Provider.NAVER;
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
                "https://nid.naver.com/oauth2.0/token",
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
                "https://openapi.naver.com/v1/nid/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        ).getBody();

        Map<String, Object> naverResponse = (Map<String, Object>) response.get("response");
        String providerId = (String) naverResponse.get("id");
        String email = (String) naverResponse.get("email");
        String nickname = (String) naverResponse.get("nickname");
        String profileImageUrl = (String) naverResponse.get("profile_image");

        return new OAuthUserInfo(providerId, email, nickname, profileImageUrl, Provider.NAVER);
    }
}