package com.dongjin.tastingnote.note.service;

import com.dongjin.tastingnote.alcohol.entity.Alcohol;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRepository;
import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
import com.dongjin.tastingnote.flavor.entity.FlavorSuggestion;
import com.dongjin.tastingnote.flavor.repository.FlavorSuggestionRepository;
import com.dongjin.tastingnote.note.dto.NoteCreateRequest;
import com.dongjin.tastingnote.note.dto.NoteResponse;
import com.dongjin.tastingnote.note.dto.NoteUpdateRequest;
import com.dongjin.tastingnote.note.entity.FlavorType;
import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.entity.NoteFlavor;
import com.dongjin.tastingnote.note.entity.NoteStatus;
import com.dongjin.tastingnote.note.repository.NoteFlavorRepository;
import com.dongjin.tastingnote.note.repository.NoteImageRepository;
import com.dongjin.tastingnote.note.repository.NoteRepository;
import com.dongjin.tastingnote.report.repository.ReportRepository;
import com.dongjin.tastingnote.tag.repository.NoteTagRepository;
import com.dongjin.tastingnote.user.entity.User;
import com.dongjin.tastingnote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteFlavorRepository noteFlavorRepository;
    private final NoteImageRepository noteImageRepository;
    private final UserRepository userRepository;
    private final AlcoholRepository alcoholRepository;
    private final FlavorSuggestionRepository flavorSuggestionRepository;
    private final ReportRepository reportRepository;
    private final NoteTagRepository noteTagRepository;

    // rating 0.5 단위 검증 (1.0, 1.5, 2.0 ... 5.0)
    private void validateRating(Double rating) {
        if (Math.round(rating * 10) % 5 != 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    // 노트 생성 (기본 임시저장 상태)
    @Transactional
    public NoteResponse createNote(Long userId, NoteCreateRequest request) {
        validateRating(request.getRating());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Alcohol alcohol = alcoholRepository.findById(request.getAlcoholId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ALCOHOL_NOT_FOUND));

        Note note = Note.builder()
                .user(user)
                .alcohol(alcohol)
                .title(request.getTitle())
                .pairing(request.getPairing())
                .rating(request.getRating())
                .description(request.getDescription())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .drankAt(request.getDrankAt())
                .location(request.getLocation())
                .build();

        noteRepository.save(note);
        saveFlavors(note, request.getTasteIds(), request.getAromaIds());

        List<NoteFlavor> flavors = noteFlavorRepository.findAllByNoteId(note.getId());
        return NoteResponse.from(note, flavors);
    }

    // 노트 단건 조회
    public NoteResponse getNote(Long requesterId, Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));

        boolean isOwner = requesterId != null && note.getUser().getId().equals(requesterId);

        if (note.getStatus() == NoteStatus.DRAFT && !isOwner) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }
        if (!note.getIsPublic() && !isOwner) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        List<NoteFlavor> flavors = noteFlavorRepository.findAllByNoteId(noteId);
        return NoteResponse.from(note, flavors);
    }

    // 내 노트 전체 조회
    public List<NoteResponse> getMyNotes(Long userId) {
        return noteRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(note -> NoteResponse.from(note, noteFlavorRepository.findAllByNoteId(note.getId())))
                .toList();
    }

    // 내 노트 상태별 조회 (DRAFT or PUBLISHED)
    public List<NoteResponse> getMyNotesByStatus(Long userId, NoteStatus status) {
        return noteRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId, status).stream()
                .map(note -> NoteResponse.from(note, noteFlavorRepository.findAllByNoteId(note.getId())))
                .toList();
    }

    // 공개 노트 전체 조회 (소셜 피드)
    public List<NoteResponse> getPublicNotes() {
        return noteRepository.findAllByIsPublicTrueAndStatusOrderByCreatedAtDesc(NoteStatus.PUBLISHED).stream()
                .map(note -> NoteResponse.from(note, noteFlavorRepository.findAllByNoteId(note.getId())))
                .toList();
    }

    // 노트 수정
    @Transactional
    public NoteResponse updateNote(Long userId, Long noteId, NoteUpdateRequest request) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        if (!note.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }
        validateRating(request.getRating());

        Alcohol alcohol = alcoholRepository.findById(request.getAlcoholId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ALCOHOL_NOT_FOUND));

        note.update(
                alcohol,
                request.getTitle(),
                request.getPairing(),
                request.getRating(),
                request.getDescription(),
                request.getIsPublic(),
                request.getDrankAt(),
                request.getLocation()
        );

        noteFlavorRepository.deleteAllByNoteId(noteId);
        saveFlavors(note, request.getTasteIds(), request.getAromaIds());

        List<NoteFlavor> flavors = noteFlavorRepository.findAllByNoteId(noteId);
        return NoteResponse.from(note, flavors);
    }

    // 노트 발행
    @Transactional
    public NoteResponse publishNote(Long userId, Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        if (!note.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }
        note.publish();
        List<NoteFlavor> flavors = noteFlavorRepository.findAllByNoteId(noteId);
        return NoteResponse.from(note, flavors);
    }

    // 노트 삭제
    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        if (!note.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }
        reportRepository.deleteAllByNoteId(noteId);
        noteImageRepository.deleteAllByNoteId(noteId);
        noteFlavorRepository.deleteAllByNoteId(noteId);
        noteTagRepository.deleteAllByNoteId(noteId);
        noteRepository.delete(note);
    }

    // FlavorSuggestion ID 목록으로 NoteFlavor 저장
    private void saveFlavors(Note note, List<Long> tasteIds, List<Long> aromaIds) {
        List<Long> distinctTasteIds = tasteIds.stream().distinct().toList();
        List<Long> distinctAromaIds = aromaIds.stream().distinct().toList();

        // taste + aroma 전체 ID를 중복 없이 한 번에 조회
        Set<Long> allIds = new LinkedHashSet<>();
        allIds.addAll(distinctTasteIds);
        allIds.addAll(distinctAromaIds);

        if (allIds.isEmpty()) {
            return;
        }

        List<FlavorSuggestion> found = flavorSuggestionRepository.findAllById(allIds);

        // 존재하지 않는 ID가 하나라도 있으면 에러
        if (found.size() != allIds.size()) {
            throw new BusinessException(ErrorCode.FLAVOR_NOT_FOUND);
        }

        Map<Long, FlavorSuggestion> flavorMap = found.stream()
                .collect(Collectors.toMap(FlavorSuggestion::getId, f -> f));

        List<NoteFlavor> noteFlavors = new ArrayList<>();

        for (Long flavorId : distinctTasteIds) {
            noteFlavors.add(NoteFlavor.builder()
                    .note(note)
                    .flavor(flavorMap.get(flavorId))
                    .type(FlavorType.TASTE)
                    .build());
        }
        for (Long flavorId : distinctAromaIds) {
            noteFlavors.add(NoteFlavor.builder()
                    .note(note)
                    .flavor(flavorMap.get(flavorId))
                    .type(FlavorType.AROMA)
                    .build());
        }

        noteFlavorRepository.saveAll(noteFlavors);
    }
}