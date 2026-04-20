package com.dongjin.tastingnote.common.notification;

import com.dongjin.tastingnote.feedback.entity.Feedback;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationService implements NotificationPort {

    private final RestTemplate restTemplate;

    @Value("${notification.slack.error-webhook-url:}")
    private String errorWebhookUrl;

    @Value("${notification.slack.feedback-webhook-url:}")
    private String feedbackWebhookUrl;

    @Override
    public void sendError(Exception e, HttpServletRequest request, HttpStatus status) {
        if (errorWebhookUrl == null || errorWebhookUrl.isBlank()) {
            return;
        }

        String time = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        boolean isServerError = status.is5xxServerError();
        String emoji = isServerError ? "🚨" : "⚠️";
        String level = isServerError ? "서버 에러" : "클라이언트 에러";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s *[%d] %s*\n", emoji, status.value(), level));
        sb.append(String.format("• 시각: %s\n", time));
        sb.append(String.format("• 메서드: %s\n", request.getMethod()));
        sb.append(String.format("• URL: %s\n", request.getRequestURI()));
        sb.append(String.format("• 에러: %s\n", e.getClass().getSimpleName()));
        sb.append(String.format("• 메시지: %s", e.getMessage()));

        if (isServerError) {
            String trace = formatStackTrace(e);
            if (!trace.isBlank()) {
                sb.append("\n• 스택트레이스:\n```\n").append(trace).append("\n```");
            }
        }

        send(errorWebhookUrl, sb.toString());
    }

    @Override
    public void sendFeedback(Feedback feedback) {
        if (feedbackWebhookUrl == null || feedbackWebhookUrl.isBlank()) {
            return;
        }

        String time = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String userInfo = feedback.getUser() != null
                ? feedback.getUser().getNickname() + " (userId: " + feedback.getUser().getId() + ")"
                : "익명";

        String message = String.format(
                """
                📬 *새 피드백 도착*
                • 카테고리: %s
                • 내용: %s
                • 유저: %s
                • 앱 버전: %s
                • 시각: %s
                """,
                feedback.getCategory(),
                feedback.getContent(),
                userInfo,
                feedback.getAppVersion() != null ? feedback.getAppVersion() : "미입력",
                time
        );

        send(feedbackWebhookUrl, message);
    }

    private String formatStackTrace(Exception e) {
        return Arrays.stream(e.getStackTrace())
                .filter(f -> f.getClassName().startsWith("com.dongjin"))
                .limit(8)
                .map(f -> "  at " + f)
                .collect(Collectors.joining("\n"));
    }

    private void send(String webhookUrl, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(Map.of("text", message), headers);
            restTemplate.postForEntity(webhookUrl, httpEntity, String.class);
        } catch (Exception ex) {
            log.warn("Slack 알림 전송 실패: {}", ex.getMessage());
        }
    }
}
