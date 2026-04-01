package com.dongjin.tastingnote.note.controller;

import com.dongjin.tastingnote.common.response.ApiResponse;
import com.dongjin.tastingnote.note.dto.NoteCreateRequest;
import com.dongjin.tastingnote.note.dto.NoteResponse;
import com.dongjin.tastingnote.note.dto.NoteUpdateRequest;
import com.dongjin.tastingnote.note.entity.NoteStatus;
import com.dongjin.tastingnote.note.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    // 노트 생성
    @PostMapping
    public ResponseEntity<ApiResponse<NoteResponse>> createNote(@Valid @RequestBody NoteCreateRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(noteService.createNote(userId, request)));
    }

    // 노트 단건 조회
    @GetMapping("/{noteId}")
    public ResponseEntity<ApiResponse<NoteResponse>> getNote(@PathVariable Long noteId) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.getNote(noteId)));
    }

    // 내 노트 조회 (전체 or 상태별)
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

    // 공개 노트 조회 (소셜 피드)
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> getPublicNotes() {
        return ResponseEntity.ok(ApiResponse.ok(noteService.getPublicNotes()));
    }

    // 노트 수정
    @PatchMapping("/{noteId}")
    public ResponseEntity<ApiResponse<NoteResponse>> updateNote(
            @PathVariable Long noteId,
            @Valid @RequestBody NoteUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.updateNote(noteId, request)));
    }

    // 노트 발행
    @PatchMapping("/{noteId}/publish")
    public ResponseEntity<ApiResponse<NoteResponse>> publishNote(@PathVariable Long noteId) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.publishNote(noteId)));
    }

    // 임시저장으로 되돌리기
    @PatchMapping("/{noteId}/unpublish")
    public ResponseEntity<ApiResponse<NoteResponse>> unpublishNote(@PathVariable Long noteId) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.unpublishNote(noteId)));
    }

    // 노트 삭제
    @DeleteMapping("/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(@PathVariable Long noteId) {
        noteService.deleteNote(noteId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}