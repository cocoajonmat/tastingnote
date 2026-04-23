package com.dongjin.tastingnote.oauth.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public abstract class AbstractOAuthClient implements OAuthClient {

    protected final RestTemplate restTemplate;

    protected AbstractOAuthClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    protected String fetchAccessToken(String tokenUrl, String clientId, String clientSecret, String code, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        Map<?, ?> response = restTemplate.postForObject(
                tokenUrl,
                new HttpEntity<>(body, headers),
                Map.class
        );
        return (String) response.get("access_token");
    }

    protected Map<?, ?> fetchUserInfoMap(String userInfoUrl, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        return restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        ).getBody();
    }
}