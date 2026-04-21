package com.dongjin.tastingnote.note.service;

import com.dongjin.tastingnote.alcohol.entity.Alcohol;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRepository;
import com.dongjin.tastingnote.common.cursor.CursorUtils;
import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
import com.dongjin.tastingnote.common.response.CursorPageResponse;
import com.dongjin.tastingnote.common.response.OffsetPageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

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

    private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

    private void validateRating(BigDecimal rating) {
        if (rating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    @Transactional
    public NoteResponse createNote(Long userId, NoteCreateRequest request) {
        validateRating(request.getRating());
        validateAlcoholInput(request.getAlcoholId(), request.getCustomAlcoholName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Alcohol alcohol = resolveAlcohol(request.getAlcoholId());
        Note note = buildNote(user, alcohol, request);
        noteRepository.save(note);

        return toResponse(note, List.of());
    }

    public NoteResponse getNote(Long requesterId, Long noteId) {
        Note note = noteRepository.findByIdWithAlcoholAndUser(noteId)
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

    public OffsetPageResponse<NoteResponse> getMyNotesPaged(Long userId, NoteStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Note> notePage = status != null
                ? noteRepository.findMyNotesPagedByStatus(userId, status, pageable)
                : noteRepository.findMyNotesPaged(userId, pageable);

        List<NoteResponse> content = toResponseList(notePage.getContent());
        return new OffsetPageResponse<>(
                content,
                notePage.getNumber(),
                notePage.getSize(),
                notePage.getTotalElements(),
                notePage.getTotalPages(),
                notePage.hasNext()
        );
    }

    public CursorPageResponse<NoteResponse> getPublicNotesCursor(String cursor, int size, String sort) {
        return switch (sort) {
            case "popular" -> getPublicPopular(cursor, size);
            case "hot" -> getPublicHot(cursor, size);
            default -> getPublicLatest(cursor, size);
        };
    }

    private CursorPageResponse<NoteResponse> getPublicLatest(String cursor, int size) {
        long cursorId = CursorUtils.parseLongId(cursor);
        List<Note> notes = noteRepository.findPublicLatestByCursor(
                cursorId, NoteStatus.PUBLISHED, PageRequest.of(0, size + 1));
        return buildCursorResponse(notes, size,
                last -> CursorUtils.encode(Map.of("id", String.valueOf(last.getId()))));
    }

    private CursorPageResponse<NoteResponse> getPublicPopular(String cursor, int size) {
        int cursorLikeCount = Integer.MAX_VALUE;
        long cursorId = Long.MAX_VALUE;
        if (cursor != null) {
            Map<String, String> params = CursorUtils.decode(cursor);
            cursorLikeCount = Integer.parseInt(params.get("likeCount"));
            cursorId = Long.parseLong(params.get("id"));
        }
        List<Note> notes = noteRepository.findPublicPopularByCursor(
                cursorLikeCount, cursorId, NoteStatus.PUBLISHED, PageRequest.of(0, size + 1));
        return buildCursorResponse(notes, size, last -> CursorUtils.encode(Map.of(
                "likeCount", String.valueOf(last.getLikeCount()),
                "id", String.valueOf(last.getId()))));
    }

    private CursorPageResponse<NoteResponse> getPublicHot(String cursor, int size) {
        double cursorHotScore = Double.MAX_VALUE;
        long cursorId = Long.MAX_VALUE;
        if (cursor != null) {
            Map<String, String> params = CursorUtils.decode(cursor);
            int likeCount = Integer.parseInt(params.get("likeCount"));
            LocalDateTime createdAt = LocalDateTime.parse(params.get("createdAt"));
            long hoursAgo = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
            cursorHotScore = likeCount / Math.pow(hoursAgo + 2.0, 1.5);
            cursorId = Long.parseLong(params.get("id"));
        }

        List<Long> ids = noteRepository.findPublicHotIdsByCursor(cursorHotScore, cursorId, size + 1);
        boolean hasNext = ids.size() > size;
        List<Long> pageIds = hasNext ? ids.subList(0, size) : ids;

        if (pageIds.isEmpty()) {
            return new CursorPageResponse<>(List.of(), null, false);
        }

        Map<Long, Note> noteMap = noteRepository.findByIdInWithAlcoholAndUser(pageIds).stream()
                .collect(Collectors.toMap(Note::getId, Function.identity()));
        List<Note> notes = pageIds.stream()
                .map(noteMap::get)
                .filter(Objects::nonNull)
                .toList();

        List<NoteResponse> content = toResponseList(notes);
        String nextCursor = hasNext ? buildHotCursor(notes.get(notes.size() - 1)) : null;
        return new CursorPageResponse<>(content, nextCursor, hasNext);
    }

    private String buildHotCursor(Note last) {
        return CursorUtils.encode(Map.of(
                "likeCount", String.valueOf(last.getLikeCount()),
                "createdAt", last.getCreatedAt().toString(),
                "id", String.valueOf(last.getId())));
    }

    private CursorPageResponse<NoteResponse> buildCursorResponse(
            List<Note> notes, int size, Function<Note, String> cursorBuilder) {
        boolean hasNext = notes.size() > size;
        List<Note> page = hasNext ? notes.subList(0, size) : notes;
        List<NoteResponse> content = toResponseList(page);
        String nextCursor = hasNext ? cursorBuilder.apply(page.get(page.size() - 1)) : null;
        return new CursorPageResponse<>(content, nextCursor, hasNext);
    }

    @Transactional
    public NoteResponse updateNote(Long userId, Long noteId, NoteUpdateRequest request) {
        Note note = findNoteAndValidateOwner(noteId, userId);
        validateRating(request.getRating());
        validateAlcoholInput(request.getAlcoholId(), request.getCustomAlcoholName());

        Alcohol alcohol = resolveAlcohol(request.getAlcoholId());
        note.update(
                alcohol,
                request.getCustomAlcoholName(),
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

    @Transactional
    public NoteResponse publishNote(Long userId, Long noteId) {
        Note note = findNoteAndValidateOwner(noteId, userId);
        note.publish();
        return toResponse(note);
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        Note note = findNoteAndValidateOwner(noteId, userId);
        List<NoteImage> images = noteImageRepository.findAllByNoteId(noteId);
        deleteImagesFromS3(images);
        reportRepository.deleteAllByNoteId(noteId);
        noteImageRepository.deleteAllByNoteId(noteId);
        noteFlavorRepository.deleteAllByNoteId(noteId);
        noteTagRepository.deleteAllByNoteId(noteId);
        noteRepository.delete(note);
    }

    private void validateAlcoholInput(Long alcoholId, String customAlcoholName) {
        boolean hasAlcoholId = alcoholId != null;
        boolean hasCustomName = customAlcoholName != null && !customAlcoholName.isBlank();
        if (!hasAlcoholId && !hasCustomName) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private Alcohol resolveAlcohol(Long alcoholId) {
        if (alcoholId == null) return null;
        return alcoholRepository.findById(alcoholId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALCOHOL_NOT_FOUND));
    }

    private Note buildNote(User user, Alcohol alcohol, NoteBaseRequest request) {
        return Note.builder()
                .user(user)
                .alcohol(alcohol)
                .customAlcoholName(alcohol == null ? request.getCustomAlcoholName() : null)
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

    private NoteResponse toResponse(Note note) {
        List<NoteImage> images = noteImageRepository.findAllByNoteId(note.getId());
        return NoteResponse.from(note, images);
    }

    private NoteResponse toResponse(Note note, List<NoteImage> images) {
        return NoteResponse.from(note, images);
    }

    private List<NoteResponse> toResponseList(List<Note> notes) {
        if (notes.isEmpty()) return List.of();
        List<Long> noteIds = notes.stream().map(Note::getId).toList();
        Map<Long, List<NoteImage>> imageMap = noteImageRepository.findAllByNoteIdIn(noteIds)
                .stream().collect(groupingBy(img -> img.getNote().getId()));
        return notes.stream()
                .map(n -> NoteResponse.from(n, imageMap.getOrDefault(n.getId(), List.of())))
                .toList();
    }

    private Note findNoteAndValidateOwner(Long noteId, Long userId) {
        Note note = noteRepository.findByIdWithAlcoholAndUser(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        if (!note.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }
        return note;
    }

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

    private void deleteImagesFromS3(List<NoteImage> images) {
        images.forEach(img -> {
            String url = img.getImageUrl();
            String key = url.substring(url.indexOf(".amazonaws.com/") + ".amazonaws.com/".length());
            s3Port.delete(key);
        });
    }
}