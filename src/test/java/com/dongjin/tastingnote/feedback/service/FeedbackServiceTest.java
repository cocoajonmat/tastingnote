package com.dongjin.tastingnote.feedback.service;

import com.dongjin.tastingnote.common.notification.NotificationPort;
import com.dongjin.tastingnote.feedback.dto.FeedbackRequest;
import com.dongjin.tastingnote.feedback.entity.Feedback;
import com.dongjin.tastingnote.feedback.entity.FeedbackCategory;
import com.dongjin.tastingnote.feedback.repository.FeedbackRepository;
import com.dongjin.tastingnote.user.entity.Provider;
import com.dongjin.tastingnote.user.entity.User;
import com.dongjin.tastingnote.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationPort notificationPort;
    // NotificationPort는 인터페이스 → Mock으로 만들면 SlackNotificationService 없이도 테스트 가능
    // 이게 Port 인터페이스를 분리한 핵심 이유 중 하나

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    @DisplayName("로그인 유저가 피드백 제출 시 저장 + Slack 알림이 호출된다")
    void submit_withLoggedInUser_savesAndNotifies() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .email("user@test.com")
                .nickname("테스터")
                .provider(Provider.LOCAL)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        FeedbackRequest request = new FeedbackRequest();
        ReflectionTestUtils.setField(request, "category", FeedbackCategory.BUG);
        ReflectionTestUtils.setField(request, "content", "버튼이 작동 안 해요");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // feedbackRepository.save()가 호출될 때 전달된 Feedback을 그대로 반환하도록 설정
        // any(Feedback.class): "어떤 Feedback 객체든 매칭"
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        feedbackService.submit(userId, request);

        // then
        // verify(mock, times(1)): 해당 메서드가 정확히 1번 호출됐는지 확인
        verify(feedbackRepository, times(1)).save(any(Feedback.class));
        verify(notificationPort, times(1)).sendFeedback(any(Feedback.class));
    }

    @Test
    @DisplayName("비로그인 유저(userId=null)가 피드백 제출 시 user=null로 저장되고 Slack 알림 호출")
    void submit_withAnonymousUser_savesWithNullUserAndNotifies() {
        // given
        FeedbackRequest request = new FeedbackRequest();
        ReflectionTestUtils.setField(request, "category", FeedbackCategory.FEEDBACK);
        ReflectionTestUtils.setField(request, "content", "UI가 불편해요");

        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when: userId = null (비로그인)
        feedbackService.submit(null, request);

        // then: userRepository는 호출되지 않아야 함
        verify(userRepository, never()).findById(any());
        verify(feedbackRepository, times(1)).save(any(Feedback.class));
        verify(notificationPort, times(1)).sendFeedback(any(Feedback.class));
    }

    @Test
    @DisplayName("저장된 Feedback의 user와 category가 올바르게 설정된다")
    void submit_withLoggedInUser_feedbackHasCorrectFields() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .email("user@test.com")
                .nickname("테스터")
                .provider(Provider.LOCAL)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        FeedbackRequest request = new FeedbackRequest();
        ReflectionTestUtils.setField(request, "category", FeedbackCategory.SUGGESTION);
        ReflectionTestUtils.setField(request, "content", "다크모드 추가해주세요");
        ReflectionTestUtils.setField(request, "appVersion", "1.0.0");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ArgumentCaptor: save()에 실제로 전달된 객체를 캡처해서 내용을 검증
        // → "저장할 때 어떤 값이 들어갔는지" 확인할 수 있음
        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);

        // when
        feedbackService.submit(userId, request);

        // then
        verify(feedbackRepository).save(captor.capture());
        Feedback saved = captor.getValue();

        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getCategory()).isEqualTo(FeedbackCategory.SUGGESTION);
        assertThat(saved.getContent()).isEqualTo("다크모드 추가해주세요");
        assertThat(saved.getAppVersion()).isEqualTo("1.0.0");
    }
}