package com.dongjin.tastingnote.note.controller;

import com.dongjin.tastingnote.common.response.ApiResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "노트", description = "노트 작성, 조회, 수정, 삭제 관련 API")
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @Operation(summary = "노트 생성", description = "새 노트를 생성합니다. status 필드로 DRAFT(임시저장) 또는 PUBLISHED(발행) 선택 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<NoteResponse>> createNote(@Valid @RequestBody NoteCreateRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(noteService.createNote(userId, request)));
    }

    @Operation(summary = "노트 단건 조회", description = "noteId로 특정 노트를 조회합니다. 공개 노트는 비로그인도 조회 가능하며, 비공개/임시저장 노트는 본인만 조회 가능합니다.")
    @GetMapping("/{noteId}")
    public ResponseEntity<ApiResponse<NoteResponse>> getNote(@PathVariable Long noteId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (auth != null && auth.getPrincipal() instanceof Long)
                ? (Long) auth.getPrincipal()
                : null;
        return ResponseEntity.ok(ApiResponse.ok(noteService.getNote(userId, noteId)));
    }

    @Operation(summary = "내 노트 목록 조회", description = "내 노트 전체 또는 상태별로 조회합니다. status 파라미터 없으면 전체 조회, DRAFT 또는 PUBLISHED로 필터링 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> getMyNotes(
            @RequestParam(required = false) NoteStatus status
    ) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (status != null) {
            return ResponseEntity.ok(ApiResponse.ok(noteService.getMyNotesByStatus(userId, status)));
        }
        return ResponseEntity.ok(ApiResponse.ok(noteService.getMyNotes(userId)));
    }

    @Operation(summary = "공개 노트 피드 조회", description = "모든 유저의 공개(isPublic=true, PUBLISHED) 노트를 조회합니다.")
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> getPublicNotes() {
        return ResponseEntity.ok(ApiResponse.ok(noteService.getPublicNotes()));
    }

    @Operation(summary = "노트 수정", description = "노트 내용을 수정합니다. 본인의 노트만 수정 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{noteId}")
    public ResponseEntity<ApiResponse<NoteResponse>> updateNote(
            @PathVariable Long noteId,
            @Valid @RequestBody NoteUpdateRequest request
    ) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(noteService.updateNote(userId, noteId, request)));
    }

    @Operation(summary = "노트 발행", description = "DRAFT 상태의 노트를 PUBLISHED 상태로 변경합니다. 본인의 노트만 발행 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{noteId}/publish")
    public ResponseEntity<ApiResponse<NoteResponse>> publishNote(@PathVariable Long noteId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(noteService.publishNote(userId, noteId)));
    }

    @Operation(summary = "노트 임시저장으로 되돌리기", description = "PUBLISHED 상태의 노트를 DRAFT 상태로 되돌립니다. 본인의 노트만 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{noteId}/unpublish")
    public ResponseEntity<ApiResponse<NoteResponse>> unpublishNote(@PathVariable Long noteId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(noteService.unpublishNote(userId, noteId)));
    }

    @Operation(summary = "노트 삭제", description = "노트를 삭제합니다. 본인의 노트만 삭제 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(@PathVariable Long noteId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        noteService.deleteNote(userId, noteId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}