package com.dongjin.tastingnote.common.notification;

import com.dongjin.tastingnote.feedback.entity.Feedback;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

public interface NotificationPort {

    void sendError(Exception e, HttpServletRequest request, HttpStatus status);

    void sendFeedback(Feedback feedback);
}
