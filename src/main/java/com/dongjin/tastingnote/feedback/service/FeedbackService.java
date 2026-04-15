package com.dongjin.tastingnote.feedback.service;

import com.dongjin.tastingnote.common.notification.NotificationPort;
import com.dongjin.tastingnote.feedback.dto.FeedbackRequest;
import com.dongjin.tastingnote.feedback.entity.Feedback;
import com.dongjin.tastingnote.feedback.repository.FeedbackRepository;
import com.dongjin.tastingnote.user.entity.User;
import com.dongjin.tastingnote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final NotificationPort notificationPort;

    @Transactional
    public void submit(Long userId, FeedbackRequest request) {
        User user = userId != null
                ? userRepository.findById(userId).orElse(null)
                : null;

        Feedback feedback = Feedback.builder()
                .user(user)
                .category(request.getCategory())
                .content(request.getContent())
                .appVersion(request.getAppVersion())
                .build();

        feedbackRepository.save(feedback);
        notificationPort.sendFeedback(feedback);
    }
}
