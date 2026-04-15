package com.dongjin.tastingnote.note.controller;

import com.dongjin.tastingnote.common.resolver.CurrentUserId;
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
import org.springframework.web.bind.annotation.*;

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
            @Valid @RequestBody NoteCreateRequest request
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

    @Operation(summary = "내 노트 목록 조회", description = "내 노트 전체 또는 상태별로 조회합니다. status 파라미터 없으면 전체 조회, DRAFT 또는 PUBLISHED로 필터링 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ResponseEntity<List<NoteResponse>> getMyNotes(
            @CurrentUserId Long userId,
            @RequestParam(required = false) NoteStatus status
    ) {
        if (status != null) {
            return ResponseEntity.ok(noteService.getMyNotesByStatus(userId, status));
        }
        return ResponseEntity.ok(noteService.getMyNotes(userId));
    }

    @Operation(summary = "공개 노트 피드 조회", description = "모든 유저의 공개(isPublic=true, PUBLISHED) 노트를 조회합니다.")
    @GetMapping("/public")
    public ResponseEntity<List<NoteResponse>> getPublicNotes() {
        return ResponseEntity.ok(noteService.getPublicNotes());
    }

    @Operation(summary = "노트 수정", description = "노트 내용을 수정합니다. 본인의 노트만 수정 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{noteId}")
    public ResponseEntity<NoteResponse> updateNote(
            @CurrentUserId Long userId,
            @PathVariable Long noteId,
            @Valid @RequestBody NoteUpdateRequest request
    ) {
        return ResponseEntity.ok(noteService.updateNote(userId, noteId, request));
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
