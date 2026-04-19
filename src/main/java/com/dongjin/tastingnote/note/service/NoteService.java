package com.dongjin.tastingnote.note.service;

import com.dongjin.tastingnote.alcohol.entity.Alcohol;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRepository;
import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
import com.dongjin.tastingnote.common.s3.S3Port;
import com.dongjin.tastingnote.note.dto.NoteBaseRequest;
import com.dongjin.tastingnote.note.dto.NoteCreateRequest;
import com.dongjin.tastingnote.note.dto.NoteResponse;
import com.dongjin.tastingnote.note.dto.NoteUpdateRequest;
import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.entity.NoteImage;
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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteFlavorRepository noteFlavorRepository;
    private final NoteImageRepository noteImageRepository;
    private final UserRepository userRepository;
    private final AlcoholRepository alcoholRepository;
    private final ReportRepository reportRepository;
    private final NoteTagRepository noteTagRepository;
    private final S3Port s3Port;

    // rating 0.5 단위 검증 (1.0, 1.5, 2.0 ... 5.0)
    private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

    private void validateRating(BigDecimal rating) {
        if (rating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) != 0) {
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

        Note note = buildNote(user, alcohol, request);
        noteRepository.save(note);

        return toResponse(note, List.of());
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

        return toResponse(note);
    }

    // 내 노트 전체 조회
    public List<NoteResponse> getMyNotes(Long userId) {
        return noteRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    // 내 노트 상태별 조회 (DRAFT or PUBLISHED)
    public List<NoteResponse> getMyNotesByStatus(Long userId, NoteStatus status) {
        return noteRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId, status).stream()
                .map(this::toResponse)
                .toList();
    }

    // 공개 노트 전체 조회 (소셜 피드)
    public List<NoteResponse> getPublicNotes() {
        return noteRepository.findAllByIsPublicTrueAndStatusOrderByCreatedAtDesc(NoteStatus.PUBLISHED).stream()
                .map(this::toResponse)
                .toList();
    }

    // 노트 수정
    @Transactional
    public NoteResponse updateNote(Long userId, Long noteId, NoteUpdateRequest request) {
        Note note = findNoteAndValidateOwner(noteId, userId);
        validateRating(request.getRating());

        Alcohol alcohol = alcoholRepository.findById(request.getAlcoholId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ALCOHOL_NOT_FOUND));

        note.update(
                alcohol,
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

        return toResponse(note);
    }

    // 노트 이미지 교체
    @Transactional
    public NoteResponse updateImages(Long userId, Long noteId, List<MultipartFile> images) {
        Note note = findNoteAndValidateOwner(noteId, userId);

        List<MultipartFile> nonEmpty = images.stream().filter(f -> !f.isEmpty()).toList();
        if (nonEmpty.size() > 3) throw new BusinessException(ErrorCode.IMAGE_LIMIT_EXCEEDED);

        List<NoteImage> oldImages = noteImageRepository.findAllByNoteId(noteId);
        deleteImagesFromS3(oldImages);
        noteImageRepository.deleteAllByNoteId(noteId);

        List<NoteImage> savedImages = saveImages(note, nonEmpty);
        return toResponse(note, savedImages);
    }

    // 노트 발행
    @Transactional
    public NoteResponse publishNote(Long userId, Long noteId) {
        Note note = findNoteAndValidateOwner(noteId, userId);
        note.publish();
        return toResponse(note);
    }

    // 노트 삭제
    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        Note note = findNoteAndValidateOwner(noteId, userId);
        List<NoteImage> images = noteImageRepository.findAllByNoteId(noteId); // URL 확보 먼저
        deleteImagesFromS3(images);
        reportRepository.deleteAllByNoteId(noteId);
        noteImageRepository.deleteAllByNoteId(noteId);
        noteFlavorRepository.deleteAllByNoteId(noteId);
        noteTagRepository.deleteAllByNoteId(noteId);
        noteRepository.delete(note);
    }

    // Note.builder() 공통 헬퍼
    private Note buildNote(User user, Alcohol alcohol, NoteBaseRequest request) {
        return Note.builder()
                .user(user)
                .alcohol(alcohol)
                .title(request.getTitle())
                .taste(request.getTaste())
                .aroma(request.getAroma())
                .pairing(request.getPairing())
                .rating(request.getRating())
                .description(request.getDescription())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .drankAt(request.getDrankAt())
                .location(request.getLocation())
                .build();
    }

    // 읽기용 응답 헬퍼
    private NoteResponse toResponse(Note note) {
        List<NoteImage> images = noteImageRepository.findAllByNoteId(note.getId());
        return NoteResponse.from(note, images);
    }

    // 쓰기 후 응답 헬퍼 — 이미지 리스트 재사용
    private NoteResponse toResponse(Note note, List<NoteImage> images) {
        return NoteResponse.from(note, images);
    }

    // 노트 조회 + 소유자 검증 공통 헬퍼
    private Note findNoteAndValidateOwner(Long noteId, Long userId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        if (!note.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }
        return note;
    }

    // 이미지 S3 업로드 후 NoteImage 저장 — 저장된 리스트 반환 (DB 쿼리 절약)
    private List<NoteImage> saveImages(Note note, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return List.of();

        List<MultipartFile> nonEmpty = images.stream().filter(f -> !f.isEmpty()).toList();
        if (nonEmpty.size() > 3) throw new BusinessException(ErrorCode.IMAGE_LIMIT_EXCEEDED);

        List<NoteImage> noteImages = new ArrayList<>();
        for (MultipartFile file : nonEmpty) {
            validateImageType(file);
            String ext = Optional.ofNullable(StringUtils.getFilenameExtension(file.getOriginalFilename()))
                    .orElse("jpg");
            String key = "notes/" + note.getId() + "/" + UUID.randomUUID() + "." + ext;
            String url = s3Port.upload(file, key);
            noteImages.add(NoteImage.builder().note(note).imageUrl(url).build());
        }
        return noteImageRepository.saveAll(noteImages);
    }

    private void validateImageType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_TYPE);
        }
    }

    // S3에서 이미지 파일 삭제 헬퍼
    private void deleteImagesFromS3(List<NoteImage> images) {
        images.forEach(img -> {
            String url = img.getImageUrl();
            String key = url.substring(url.indexOf(".amazonaws.com/") + ".amazonaws.com/".length());
            s3Port.delete(key);
        });
    }

}
