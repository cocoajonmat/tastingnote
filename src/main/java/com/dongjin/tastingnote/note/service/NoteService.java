package com.dongjin.tastingnote.note.service;

import com.dongjin.tastingnote.alcohol.entity.Alcohol;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRepository;
import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
import com.dongjin.tastingnote.note.dto.NoteCreateRequest;
import com.dongjin.tastingnote.note.dto.NoteResponse;
import com.dongjin.tastingnote.note.dto.NoteUpdateRequest;
import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.entity.NoteStatus;
import com.dongjin.tastingnote.note.repository.NoteRepository;
import com.dongjin.tastingnote.user.entity.User;
import com.dongjin.tastingnote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final AlcoholRepository alcoholRepository;

    // 노트 생성 (기본 임시저장 상태)
    @Transactional
    public NoteResponse createNote(Long userId, NoteCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Alcohol alcohol = null;
        if (request.getAlcoholId() != null) {
            alcohol = alcoholRepository.findById(request.getAlcoholId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ALCOHOL_NOT_FOUND));
        }

        Note note = Note.builder()
                .user(user)
                .alcohol(alcohol)
                .alcoholName(request.getAlcoholName())
                .title(request.getTitle())
                .taste(request.getTaste())
                .aroma(request.getAroma())
                .pairing(request.getPairing())
                .rating(request.getRating())
                .description(request.getDescription())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .drankAt(request.getDrankAt())
                .location(request.getLocation())
                .build();

        return NoteResponse.from(noteRepository.save(note));
    }

    // 노트 단건 조회
    public NoteResponse getNote(Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        return NoteResponse.from(note);
    }

    // 내 노트 전체 조회
    public List<NoteResponse> getMyNotes(Long userId) {
        return noteRepository.findAllByUserId(userId).stream()
                .map(NoteResponse::from)
                .toList();
    }

    // 내 노트 상태별 조회 (DRAFT or PUBLISHED)
    public List<NoteResponse> getMyNotesByStatus(Long userId, NoteStatus status) {
        return noteRepository.findAllByUserIdAndStatus(userId, status).stream()
                .map(NoteResponse::from)
                .toList();
    }

    // 공개 노트 전체 조회 (소셜 피드)
    public List<NoteResponse> getPublicNotes() {
        return noteRepository.findAllByIsPublicTrueAndStatus(NoteStatus.PUBLISHED).stream()
                .map(NoteResponse::from)
                .toList();
    }

    // 노트 수정
    @Transactional
    public NoteResponse updateNote(Long noteId, NoteUpdateRequest request) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

        note.update(
                request.getTitle(),
                request.getTaste(),
                request.getAroma(),
                request.getPairing(),
                request.getRating(),
                request.getDescription(),
                request.getIsPublic(),
                request.getDrankAt(),
                request.getLocation()
        );

        return NoteResponse.from(note);
    }

    // 노트 발행
    @Transactional
    public NoteResponse publishNote(Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        note.publish();
        return NoteResponse.from(note);
    }

    // 임시저장으로 되돌리기
    @Transactional
    public NoteResponse unpublishNote(Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        note.saveDraft();
        return NoteResponse.from(note);
    }

    // 노트 삭제
    @Transactional
    public void deleteNote(Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        noteRepository.delete(note);
    }
}