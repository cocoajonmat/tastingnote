package com.dongjin.tastingnote.event.aop;

import com.dongjin.tastingnote.event.entity.UserEvent;
import com.dongjin.tastingnote.event.entity.UserEventType;
import com.dongjin.tastingnote.event.repository.UserEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventAspect {

    private final UserEventRepository userEventRepository;

    // 술 검색 시 이벤트 기록
    @AfterReturning("execution(* com.dongjin.tastingnote.alcohol.service.AlcoholService.search(..))")
    public void recordSearch(JoinPoint joinPoint) {
        Long userId = getCurrentUserId();
        if (userId == null) return;

        String keyword = (String) joinPoint.getArgs()[0];
        String metadata = "{\"keyword\": \"" + keyword + "\"}";
        save(userId, UserEventType.SEARCH, metadata);
    }

    // 술 단건 조회 시 이벤트 기록 (Controller에서 호출될 때만 — 내부 호출 제외)
    @AfterReturning("execution(* com.dongjin.tastingnote.alcohol.controller.AlcoholController.getById(..))")
    public void recordViewAlcohol(JoinPoint joinPoint) {
        Long userId = getCurrentUserId();
        if (userId == null) return;

        Long alcoholId = (Long) joinPoint.getArgs()[0];
        String metadata = "{\"alcoholId\": " + alcoholId + "}";
        save(userId, UserEventType.VIEW_ALCOHOL, metadata);
    }

    // 노트 단건 조회 시 이벤트 기록
    @AfterReturning("execution(* com.dongjin.tastingnote.note.service.NoteService.getNote(..))")
    public void recordViewNote(JoinPoint joinPoint) {
        Long userId = getCurrentUserId();
        if (userId == null) return;

        Long noteId = (Long) joinPoint.getArgs()[0];
        String metadata = "{\"noteId\": " + noteId + "}";
        save(userId, UserEventType.VIEW_NOTE, metadata);
    }

    // 노트 작성 시 이벤트 기록 (NOTE_CREATED + NOTE_RATED 동시에)
    @AfterReturning(
            pointcut = "execution(* com.dongjin.tastingnote.note.service.NoteService.createNote(..))",
            returning = "result"
    )
    public void recordNoteCreated(JoinPoint joinPoint, Object result) {
        Long userId = getCurrentUserId();
        if (userId == null) return;

        // NoteResponse에서 alcoholId와 rating을 꺼내기 위해 리플렉션 사용
        try {
            Long alcoholId = (Long) result.getClass().getMethod("getAlcoholId").invoke(result);
            Object rating = result.getClass().getMethod("getRating").invoke(result);

            String createdMetadata = "{\"alcoholId\": " + alcoholId + "}";
            save(userId, UserEventType.NOTE_CREATED, createdMetadata);

            if (rating != null) {
                String ratedMetadata = "{\"alcoholId\": " + alcoholId + ", \"rating\": " + rating + "}";
                save(userId, UserEventType.NOTE_RATED, ratedMetadata);
            }
        } catch (Exception e) {
            log.warn("UserEvent NOTE_CREATED 기록 실패: {}", e.getMessage());
        }
    }

    private void save(Long userId, UserEventType type, String metadata) {
        try {
            userEventRepository.save(UserEvent.of(userId, type, metadata));
        } catch (Exception e) {
            log.warn("UserEvent 저장 실패 — type: {}, userId: {}, error: {}", type, userId, e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }
}