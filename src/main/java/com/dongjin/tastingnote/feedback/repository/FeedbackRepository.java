package com.dongjin.tastingnote.feedback.repository;

import com.dongjin.tastingnote.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
