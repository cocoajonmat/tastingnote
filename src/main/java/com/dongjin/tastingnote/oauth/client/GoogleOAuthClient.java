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
public class GoogleOAuthClient implements OAuthClient {

    private final RestTemplate restTemplate;

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    public GoogleOAuthClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Provider provider() {
        return Provider.GOOGLE;
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
                "https://oauth2.googleapis.com/token",
                new HttpEntity<>(body, headers),
                Map.class
        );
        return (String) response.get("access_token");
    }

    private OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        Map<?, ?> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        ).getBody();

        String providerId = (String) response.get("id");
        String email = (String) response.get("email");
        String nickname = (String) response.get("name");
        String profileImageUrl = (String) response.get("picture");

        return new OAuthUserInfo(providerId, email, nickname, profileImageUrl, Provider.GOOGLE);
    }
}