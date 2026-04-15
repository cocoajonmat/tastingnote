package com.dongjin.tastingnote.feedback.controller;

import com.dongjin.tastingnote.common.resolver.CurrentUserId;
import com.dongjin.tastingnote.feedback.dto.FeedbackRequest;
import com.dongjin.tastingnote.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "피드백", description = "버그 신고 및 피드백 제출 API")
@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "피드백 제출", description = "버그, 개선 제안, 피드백을 제출합니다. 로그인 없이도 사용 가능합니다.")
    @PostMapping
    public ResponseEntity<Void> submit(
            @CurrentUserId Long userId,
            @Valid @RequestBody FeedbackRequest request
    ) {
        feedbackService.submit(userId, request);
        return ResponseEntity.noContent().build();
    }
}
