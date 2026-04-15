package com.dongjin.tastingnote.alcohol.service;

import com.dongjin.tastingnote.alcohol.dto.AlcoholRequestCreateRequest;
import com.dongjin.tastingnote.alcohol.dto.AlcoholRequestResponse;
import com.dongjin.tastingnote.alcohol.entity.*;
import com.dongjin.tastingnote.alcohol.repository.AlcoholAliasRepository;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRepository;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRequestRepository;
import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlcoholRequestServiceTest {

    @Mock private AlcoholRequestRepository alcoholRequestRepository;
    @Mock private AlcoholRepository alcoholRepository;
    @Mock private AlcoholAliasRepository alcoholAliasRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AlcoholRequestService alcoholRequestService;

    // ──────────────────────────────────────────────
    // 공통 픽스처
    // ──────────────────────────────────────────────

    private User makeUser(Long id) {
        User user = User.builder()
                .email("user@test.com")
                .nickname("테스터")
                .provider(Provider.LOCAL)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private AlcoholRequestCreateRequest makeCreateRequest(String name) {
        AlcoholRequestCreateRequest req = new AlcoholRequestCreateRequest();
        ReflectionTestUtils.setField(req, "name", name);
        ReflectionTestUtils.setField(req, "nameKo", "버번 위스키");
        ReflectionTestUtils.setField(req, "aliases", List.of("버번", "버번위스키"));
        ReflectionTestUtils.setField(req, "reason", "노트 작성에 필요해요");
        ReflectionTestUtils.setField(req, "category", AlcoholCategory.WHISKEY);
        return req;
    }

    private AlcoholRequest makePendingRequest(Long id, User user) {
        AlcoholRequest req = AlcoholRequest.builder()
                .requestedBy(user)
                .name("Buffalo Trace")
                .nameKo("버팔로 트레이스")
                .aliases(List.of("버팔로"))
                .category(AlcoholCategory.WHISKEY)
                .build();
        ReflectionTestUtils.setField(req, "id", id);
        return req;
    }

    // ──────────────────────────────────────────────
    // request()
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("정상 요청 시 AlcoholRequest가 저장된다")
    void request_success() {
        // given
        User user = makeUser(1L);
        AlcoholRequestCreateRequest req = makeCreateRequest("Buffalo Trace");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(alcoholRequestRepository.existsByRequestedByAndNameIgnoreCase(user, "Buffalo Trace")).thenReturn(false);

        // when
        alcoholRequestService.request(1L, req);

        // then
        ArgumentCaptor<AlcoholRequest> captor = ArgumentCaptor.forClass(AlcoholRequest.class);
        verify(alcoholRequestRepository).save(captor.capture());

        AlcoholRequest saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Buffalo Trace");
        assertThat(saved.getCategory()).isEqualTo(AlcoholCategory.WHISKEY);
        assertThat(saved.getStatus()).isEqualTo(AlcoholRequestStatus.PENDING);
        assertThat(saved.getRequestedBy()).isEqualTo(user);
    }

    @Test
    @DisplayName("동일 name으로 이미 요청한 유저는 DUPLICATE_ALCOHOL_REQUEST 예외가 발생한다")
    void request_duplicate_throwsDuplicateAlcoholRequest() {
        // given
        User user = makeUser(1L);
        AlcoholRequestCreateRequest req = makeCreateRequest("Buffalo Trace");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(alcoholRequestRepository.existsByRequestedByAndNameIgnoreCase(user, "Buffalo Trace")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> alcoholRequestService.request(1L, req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_ALCOHOL_REQUEST);

        verify(alcoholRequestRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // approve()
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("approve 시 Alcohol과 AlcoholAlias가 생성되고 status가 APPROVED로 변경된다")
    void approve_success() {
        // given
        User user = makeUser(1L);
        AlcoholRequest req = makePendingRequest(10L, user);

        when(alcoholRequestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(alcoholRepository.save(any(Alcohol.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        alcoholRequestService.approve(10L);

        // then
        assertThat(req.getStatus()).isEqualTo(AlcoholRequestStatus.APPROVED);
        verify(alcoholRepository).save(any(Alcohol.class));
        // aliases가 1개이므로 AlcoholAlias도 1번 저장
        verify(alcoholAliasRepository, times(1)).save(any(AlcoholAlias.class));
    }

    @Test
    @DisplayName("이미 처리된 요청을 approve하면 ALREADY_PROCESSED 예외가 발생한다")
    void approve_alreadyProcessed_throwsAlreadyProcessed() {
        // given
        User user = makeUser(1L);
        AlcoholRequest req = makePendingRequest(10L, user);
        req.approve(); // 이미 APPROVED 상태로 만들기

        when(alcoholRequestRepository.findById(10L)).thenReturn(Optional.of(req));

        // when & then
        assertThatThrownBy(() -> alcoholRequestService.approve(10L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_PROCESSED);
    }

    // ──────────────────────────────────────────────
    // merge()
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("merge 시 기존 Alcohol에 별칭이 추가되고 status가 MERGED로 변경된다")
    void merge_success() {
        // given
        User user = makeUser(1L);
        AlcoholRequest req = makePendingRequest(10L, user);

        Alcohol existingAlcohol = Alcohol.builder()
                .name("Buffalo Trace Bourbon")
                .nameKo("버팔로 트레이스 버번")
                .category(AlcoholCategory.WHISKEY)
                .build();
        ReflectionTestUtils.setField(existingAlcohol, "id", 5L);

        when(alcoholRequestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(alcoholRepository.findById(5L)).thenReturn(Optional.of(existingAlcohol));

        // when
        alcoholRequestService.merge(10L, 5L);

        // then
        assertThat(req.getStatus()).isEqualTo(AlcoholRequestStatus.MERGED);
        assertThat(req.getMergedToAlcohol()).isEqualTo(existingAlcohol);
        verify(alcoholAliasRepository, times(1)).save(any(AlcoholAlias.class));
    }

    @Test
    @DisplayName("merge 대상 Alcohol이 없으면 ALCOHOL_NOT_FOUND 예외가 발생한다")
    void merge_alcoholNotFound_throwsAlcoholNotFound() {
        // given
        User user = makeUser(1L);
        AlcoholRequest req = makePendingRequest(10L, user);

        when(alcoholRequestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(alcoholRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> alcoholRequestService.merge(10L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALCOHOL_NOT_FOUND);
    }

    // ──────────────────────────────────────────────
    // reject()
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("reject 시 status가 REJECTED로 변경된다")
    void reject_success() {
        // given
        User user = makeUser(1L);
        AlcoholRequest req = makePendingRequest(10L, user);

        when(alcoholRequestRepository.findById(10L)).thenReturn(Optional.of(req));

        // when
        alcoholRequestService.reject(10L);

        // then
        assertThat(req.getStatus()).isEqualTo(AlcoholRequestStatus.REJECTED);
    }

    @Test
    @DisplayName("존재하지 않는 요청을 reject하면 ALCOHOL_REQUEST_NOT_FOUND 예외가 발생한다")
    void reject_notFound_throwsAlcoholRequestNotFound() {
        // given
        when(alcoholRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> alcoholRequestService.reject(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALCOHOL_REQUEST_NOT_FOUND);
    }

    // ──────────────────────────────────────────────
    // getRequests()
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("getRequests는 status별 목록을 반환하고 각 항목에 similarAlcohols가 포함된다")
    void getRequests_returnsList_withSimilarAlcohols() {
        // given
        User user = makeUser(1L);
        AlcoholRequest req = makePendingRequest(10L, user);

        Alcohol similar = Alcohol.builder()
                .name("Buffalo Trace Bourbon")
                .nameKo("버팔로 트레이스 버번")
                .category(AlcoholCategory.WHISKEY)
                .build();

        when(alcoholRequestRepository.findAllByStatusOrderByCreatedAtDesc(AlcoholRequestStatus.PENDING))
                .thenReturn(List.of(req));
        when(alcoholRepository.searchByKeyword("Buffalo Trace")).thenReturn(List.of(similar));

        // when
        List<AlcoholRequestResponse> result = alcoholRequestService.getRequests(AlcoholRequestStatus.PENDING);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Buffalo Trace");
        assertThat(result.get(0).getSimilarAlcohols()).hasSize(1);
        assertThat(result.get(0).getSimilarAlcohols().get(0).getName()).isEqualTo("Buffalo Trace Bourbon");
    }
}