package com.dongjin.tastingnote.note.controller;

import com.dongjin.tastingnote.common.resolver.CurrentUserId;
import com.dongjin.tastingnote.common.response.CursorPageResponse;
import com.dongjin.tastingnote.common.response.OffsetPageResponse;
import com.dongjin.tastingnote.note.dto.NoteCreateRequest;
import com.dongjin.tastingnote.note.dto.NoteResponse;
import com.dongjin.tastingnote.note.dto.NoteUpdateRequest;
import com.dongjin.tastingnote.note.entity.NoteStatus;
import com.dongjin.tastingnote.note.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "노트", description = "노트 작성, 조회, 수정, 삭제 관련 API")
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @Operation(summary = "노트 생성", description = "새 노트를 생성합니다. 생성 시 항상 DRAFT(임시저장) 상태로 저장되며, 발행은 /publish 엔드포인트를 사용하세요.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<NoteResponse> createNote(
            @CurrentUserId Long userId,
            @RequestBody @Valid NoteCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.createNote(userId, request));
    }

    @Operation(summary = "노트 단건 조회", description = "noteId로 특정 노트를 조회합니다. 공개 노트는 비로그인도 조회 가능하며, 비공개/임시저장 노트는 본인만 조회 가능합니다.")
    @GetMapping("/{noteId}")
    public ResponseEntity<NoteResponse> getNote(
            @CurrentUserId Long userId,
            @PathVariable Long noteId
    ) {
        return ResponseEntity.ok(noteService.getNote(userId, noteId));
    }

    @Operation(summary = "내 노트 목록 조회", description = "내 노트를 페이지 단위로 조회합니다. status 파라미터로 DRAFT/PUBLISHED 필터링 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ResponseEntity<OffsetPageResponse<NoteResponse>> getMyNotes(
            @CurrentUserId Long userId,
            @RequestParam(required = false) NoteStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(noteService.getMyNotesPaged(userId, status, page, size));
    }

    @Operation(summary = "공개 노트 피드 조회", description = "공개 노트 피드를 커서 기반으로 조회합니다. sort: latest(기본), popular, hot")
    @GetMapping("/public")
    public ResponseEntity<CursorPageResponse<NoteResponse>> getPublicNotes(
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(noteService.getPublicNotesCursor(cursor, size, sort));
    }

    @Operation(summary = "노트 수정", description = "노트 내용을 수정합니다. 본인의 노트만 수정 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{noteId}")
    public ResponseEntity<NoteResponse> updateNote(
            @CurrentUserId Long userId,
            @PathVariable Long noteId,
            @RequestBody @Valid NoteUpdateRequest request
    ) {
        return ResponseEntity.ok(noteService.updateNote(userId, noteId, request));
    }

    @Operation(summary = "노트 이미지 교체", description = "노트 이미지를 전부 교체합니다. 기존 이미지는 삭제되고 새 이미지로 대체됩니다. 최대 3장.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "/{noteId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoteResponse> updateImages(
            @CurrentUserId Long userId,
            @PathVariable Long noteId,
            @RequestPart("images") List<MultipartFile> images
    ) {
        return ResponseEntity.ok(noteService.updateImages(userId, noteId, images));
    }

    @Operation(summary = "노트 발행", description = "DRAFT 상태의 노트를 PUBLISHED 상태로 변경합니다. 본인의 노트만 발행 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{noteId}/publish")
    public ResponseEntity<NoteResponse> publishNote(
            @CurrentUserId Long userId,
            @PathVariable Long noteId
    ) {
        return ResponseEntity.ok(noteService.publishNote(userId, noteId));
    }

    @Operation(summary = "노트 삭제", description = "노트를 삭제합니다. 본인의 노트만 삭제 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @CurrentUserId Long userId,
            @PathVariable Long noteId
    ) {
        noteService.deleteNote(userId, noteId);
        return ResponseEntity.noContent().build();
    }
}