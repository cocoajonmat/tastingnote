package com.dongjin.tastingnote.common.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RestTemplate restTemplate;

    @Value("${notification.slack-webhook-url:}")
    private String slackWebhookUrl;

    public void sendSlackError(Exception e, String requestUri) {
        if (slackWebhookUrl == null || slackWebhookUrl.isBlank()) {
            return;
        }

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String message = String.format(
                """
                🚨 *서버 에러 발생*
                • 시각: %s
                • URL: %s
                • 에러: %s
                • 메시지: %s
                """,
                time, requestUri, e.getClass().getSimpleName(), e.getMessage()
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("text", message), headers);
            restTemplate.postForEntity(slackWebhookUrl, request, String.class);
        } catch (Exception ex) {
            log.warn("Slack 알림 전송 실패: {}", ex.getMessage());
        }
    }
}