package com.dongjin.tastingnote.note.service;

import com.dongjin.tastingnote.alcohol.entity.Alcohol;
import com.dongjin.tastingnote.alcohol.entity.AlcoholCategory;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRepository;
import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
import com.dongjin.tastingnote.flavor.repository.FlavorSuggestionRepository;
import com.dongjin.tastingnote.note.dto.NoteCreateRequest;
import com.dongjin.tastingnote.note.dto.NoteResponse;
import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.entity.NoteStatus;
import com.dongjin.tastingnote.note.repository.NoteFlavorRepository;
import com.dongjin.tastingnote.note.repository.NoteImageRepository;
import com.dongjin.tastingnote.note.repository.NoteRepository;
import com.dongjin.tastingnote.report.repository.ReportRepository;
import com.dongjin.tastingnote.tag.repository.NoteTagRepository;
import com.dongjin.tastingnote.user.entity.Provider;
import com.dongjin.tastingnote.user.entity.User;
import com.dongjin.tastingnote.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * NoteService 단위 테스트
 *
 * Mockito란?
 * → 실제 DB 없이 가짜(Mock) 객체를 만들어서 테스트하는 라이브러리.
 * → NoteService는 NoteRepository, UserRepository 등 여러 Repository에 의존함.
 *   실제 DB가 없어도 "이렇게 물어보면 이걸 돌려줘"라고 가짜 응답을 설정할 수 있음.
 *
 * @ExtendWith(MockitoExtension.class)
 * → JUnit5에 Mockito를 연결하는 어노테이션.
 *   이게 있어야 @Mock, @InjectMocks가 동작함.
 */
@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    /**
     * @Mock
     * → 가짜(Mock) 객체를 생성. 실제 구현체가 아니라 텅 빈 껍데기.
     * → 기본적으로 모든 메서드가 null/0/false를 반환함.
     * → when(...).thenReturn(...)으로 원하는 응답을 설정할 수 있음.
     */
    @Mock private NoteRepository noteRepository;
    @Mock private NoteFlavorRepository noteFlavorRepository;
    @Mock private NoteImageRepository noteImageRepository;
    @Mock private UserRepository userRepository;
    @Mock private AlcoholRepository alcoholRepository;
    @Mock private FlavorSuggestionRepository flavorSuggestionRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private NoteTagRepository noteTagRepository;

    /**
     * @InjectMocks
     * → 위에서 @Mock으로 만든 객체들을 NoteService 생성자에 자동으로 주입.
     * → 즉, 실제 NoteService 인스턴스가 만들어지지만
     *   내부의 모든 Repository는 가짜 Mock 객체로 채워짐.
     */
    @InjectMocks
    private NoteService noteService;

    // 테스트에서 공통으로 쓸 객체들
    private User owner;
    private User stranger;
    private Alcohol alcohol;

    @BeforeEach
    void setUp() {
        // 노트 소유자 유저
        owner = User.builder()
                .email("owner@test.com")
                .nickname("소유자")
                .provider(Provider.LOCAL)
                .build();
        // ReflectionTestUtils.setField: private 필드에 값을 강제로 넣는 테스트 유틸.
        // JPA가 자동으로 설정하는 id같은 필드는 테스트에서 이 방법으로 설정함.
        ReflectionTestUtils.setField(owner, "id", 1L);

        // 다른 유저 (소유자 아님)
        stranger = User.builder()
                .email("stranger@test.com")
                .nickname("남")
                .provider(Provider.LOCAL)
                .build();
        ReflectionTestUtils.setField(stranger, "id", 2L);

        // 테스트용 술 객체
        alcohol = Alcohol.builder()
                .name("Johnnie Walker Black")
                .nameKo("조니워커 블랙")
                .category(AlcoholCategory.WHISKEY)
                .build();
        ReflectionTestUtils.setField(alcohol, "id", 1L);
    }

    // ─────────────────────────────────────────────────
    // getNote 테스트
    // ─────────────────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 노트 조회 시 NOTE_NOT_FOUND 예외")
    void getNote_whenNoteNotFound_throwsNoteNotFound() {
        // given
        // when(가짜객체.메서드(인자)).thenReturn(반환값)
        // → "noteRepository.findById(anyLong())이 호출되면 빈 Optional을 반환해"
        // → anyLong(): "어떤 Long 값이든 매칭"을 의미하는 Mockito 매처
        when(noteRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noteService.getNote(null, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTE_NOT_FOUND);
    }

    @Test
    @DisplayName("DRAFT 노트를 소유자가 아닌 사람이 조회하면 FORBIDDEN_ACCESS")
    void getNote_whenDraftAndNotOwner_throwsForbidden() {
        // given: DRAFT 상태의 노트 (기본값이 DRAFT이므로 별도 설정 불필요)
        Note draftNote = Note.builder()
                .user(owner)
                .alcohol(alcohol)
                .title("임시저장 노트")
                .rating(new BigDecimal("4.0"))
                .build();
        // Note의 status 기본값은 DRAFT, isPublic 기본값은 false

        when(noteRepository.findById(1L)).thenReturn(Optional.of(draftNote));

        // when & then: stranger(id=2)가 조회 시도 → 403
        assertThatThrownBy(() -> noteService.getNote(2L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN_ACCESS);
    }

    @Test
    @DisplayName("비공개 PUBLISHED 노트를 소유자가 아닌 사람이 조회하면 FORBIDDEN_ACCESS")
    void getNote_whenPrivatePublishedAndNotOwner_throwsForbidden() {
        // given: PUBLISHED이지만 isPublic=false
        Note privateNote = Note.builder()
                .user(owner)
                .alcohol(alcohol)
                .title("비공개 노트")
                .rating(new BigDecimal("3.5"))
                .isPublic(false)
                .build();
        // publish() 메서드로 상태를 PUBLISHED로 변경
        privateNote.publish();

        when(noteRepository.findById(1L)).thenReturn(Optional.of(privateNote));

        // when & then
        assertThatThrownBy(() -> noteService.getNote(2L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN_ACCESS);
    }

    @Test
    @DisplayName("소유자가 자신의 DRAFT 노트를 조회하면 성공")
    void getNote_whenOwnerAccessesDraftNote_returnsResponse() {
        // given
        Note myNote = Note.builder()
                .user(owner)
                .alcohol(alcohol)
                .title("내 임시저장 노트")
                .rating(new BigDecimal("4.5"))
                .build();
        ReflectionTestUtils.setField(myNote, "id", 1L);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(myNote));
        // toResponse() 내부에서 noteFlavorRepository.findAllByNoteId()를 호출함
        // → "호출되면 빈 리스트를 반환해"
        when(noteFlavorRepository.findAllByNoteId(1L)).thenReturn(List.of());

        // when
        NoteResponse response = noteService.getNote(1L, 1L);  // 소유자(id=1)가 조회

        // then
        assertThat(response.getTitle()).isEqualTo("내 임시저장 노트");
        assertThat(response.getStatus()).isEqualTo(NoteStatus.DRAFT);
    }

    @Test
    @DisplayName("비로그인 사용자(requesterId=null)가 공개 노트를 조회하면 성공")
    void getNote_whenAnonymousAccessesPublicNote_returnsResponse() {
        // given: PUBLISHED + isPublic=true
        Note publicNote = Note.builder()
                .user(owner)
                .alcohol(alcohol)
                .title("공개 노트")
                .rating(new BigDecimal("4.0"))
                .isPublic(true)
                .build();
        publicNote.publish();
        ReflectionTestUtils.setField(publicNote, "id", 1L);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(publicNote));
        when(noteFlavorRepository.findAllByNoteId(1L)).thenReturn(List.of());

        // when: 비로그인은 requesterId = null
        NoteResponse response = noteService.getNote(null, 1L);

        // then
        assertThat(response.getIsPublic()).isTrue();
    }

    // ─────────────────────────────────────────────────
    // deleteNote 테스트 (findNoteAndValidateOwner 검증)
    // ─────────────────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 노트 삭제 시 NOTE_NOT_FOUND 예외")
    void deleteNote_whenNoteNotFound_throwsNoteNotFound() {
        // given
        when(noteRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noteService.deleteNote(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTE_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사람의 노트를 삭제하려 하면 FORBIDDEN_ACCESS")
    void deleteNote_whenNotOwner_throwsForbidden() {
        // given: 노트는 owner(id=1) 소유
        Note note = Note.builder()
                .user(owner)
                .alcohol(alcohol)
                .title("남의 노트")
                .rating(new BigDecimal("3.0"))
                .build();

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        // when & then: stranger(id=2)가 삭제 시도
        assertThatThrownBy(() -> noteService.deleteNote(2L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN_ACCESS);
    }

    @Test
    @DisplayName("소유자가 노트 삭제 시 연관 데이터 포함 전체 삭제")
    void deleteNote_whenOwner_deletesAllRelated() {
        // given
        Note note = Note.builder()
                .user(owner)
                .alcohol(alcohol)
                .title("내 노트")
                .rating(new BigDecimal("4.0"))
                .build();
        ReflectionTestUtils.setField(note, "id", 1L);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        // when
        noteService.deleteNote(1L, 1L);

        // then
        // verify: 특정 메서드가 실제로 호출됐는지 검증
        // → "진짜로 삭제 로직이 실행됐나?" 확인
        verify(reportRepository).deleteAllByNoteId(1L);
        verify(noteImageRepository).deleteAllByNoteId(1L);
        verify(noteFlavorRepository).deleteAllByNoteId(1L);
        verify(noteTagRepository).deleteAllByNoteId(1L);
        verify(noteRepository).delete(note);
    }

    // ─────────────────────────────────────────────────
    // validateRating 테스트 (createNote를 통해 간접 검증)
    // ─────────────────────────────────────────────────

    @Test
    @DisplayName("0.5 단위가 아닌 rating으로 노트 생성 시 INVALID_INPUT 예외")
    void createNote_whenInvalidRating_throwsInvalidInput() {
        // given: rating 3.3 → 0.5 단위가 아님
        NoteCreateRequest request = new NoteCreateRequest();
        ReflectionTestUtils.setField(request, "rating", new BigDecimal("3.3"));
        ReflectionTestUtils.setField(request, "tasteIds", List.of());
        ReflectionTestUtils.setField(request, "aromaIds", List.of());

        // when & then: validateRating에서 예외가 던져져야 함
        assertThatThrownBy(() -> noteService.createNote(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("유효한 rating(4.5)으로는 rating 검증을 통과한다")
    void createNote_whenValidRating_passesValidation() {
        // given: rating 4.5는 유효 (validateRating 통과 후 userRepository.findById로 진행)
        NoteCreateRequest request = new NoteCreateRequest();
        ReflectionTestUtils.setField(request, "alcoholId", 1L);
        ReflectionTestUtils.setField(request, "rating", new BigDecimal("4.5"));
        ReflectionTestUtils.setField(request, "tasteIds", List.of());
        ReflectionTestUtils.setField(request, "aromaIds", List.of());

        // userRepository.findById가 빈 Optional을 반환하도록 설정
        // → validateRating은 통과하지만 USER_NOT_FOUND에서 멈춤
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then: INVALID_INPUT이 아닌 USER_NOT_FOUND가 나와야 함
        assertThatThrownBy(() -> noteService.createNote(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);  // rating 검증은 통과했다는 증거
    }
}