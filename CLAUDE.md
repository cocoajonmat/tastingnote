# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# 컴파일 (JAVA_HOME 필수)
JAVA_HOME="/c/Users/gkehd/.jdks/temurin-21.0.5" PATH="$JAVA_HOME/bin:$PATH" ./gradlew compileJava

# 빌드
JAVA_HOME="/c/Users/gkehd/.jdks/temurin-21.0.5" PATH="$JAVA_HOME/bin:$PATH" ./gradlew build

# 테스트
JAVA_HOME="/c/Users/gkehd/.jdks/temurin-21.0.5" PATH="$JAVA_HOME/bin:$PATH" ./gradlew test

# 단일 테스트 실행
JAVA_HOME="/c/Users/gkehd/.jdks/temurin-21.0.5" PATH="$JAVA_HOME/bin:$PATH" ./gradlew test --tests "com.dongjin.tastingnote.ClassName.methodName"
```

로컬 프로파일: `application-local.yaml` (H2 인메모리 DB). `-Dspring.profiles.active=local`로 실행.

## Architecture

### 패키지 구조
도메인별 수직 슬라이스로 구성. 각 도메인 패키지는 `controller / dto / entity / repository / service`를 포함.

```
com.dongjin.tastingnote
├── alcohol       # 술 데이터 (DB 등록 술 + 사용자 요청)
├── common        # 공통 인프라 (JWT, Security, 예외, S3, 커서 페이징, Slack 알림)
├── event         # 행동 데이터 수집 (UserEvent + AOP 자동 기록)
├── feedback      # 노트 신고 기능
├── flavor        # 맛/향 태그
├── note          # 핵심 도메인 — 테이스팅 노트 CRUD + 피드
├── oauth         # 소셜 로그인 (Kakao / Google / Naver)
└── user          # 회원, 취향 카드
```

### 인증
- JWT 기반 (Access + Refresh 토큰). `JwtAuthenticationFilter`가 모든 요청에서 토큰을 검증.
- 컨트롤러 파라미터에 `@CurrentUserId Long userId`를 쓰면 JWT에서 userId가 자동 주입됨 (`CurrentUserIdArgumentResolver`).

### 예외 처리
- 비즈니스 오류는 `throw new BusinessException(ErrorCode.XXX)`로 던짐.
- `ErrorCode` enum에 HTTP 상태 + 코드 + 메시지가 정의되어 있음.
- `GlobalExceptionHandler`가 모두 잡아서 `ErrorResponse`로 직렬화.

### 페이지네이션
- 공개 피드(Latest/Popular/Hot): **커서 기반** — `CursorUtils`로 Base64 인코딩, `CursorPageResponse<T>` 반환.
- 내 노트 목록: **오프셋 기반** — Spring Data `Pageable`, `OffsetPageResponse<T>` 반환.

### Repository 규칙
- Repository 메서드 이름에 비즈니스 맥락을 넣지 않음. `findNotesByUserId`처럼 "무엇을 어떤 조건으로 가져오는가"만 표현.
- 정렬은 JPQL `ORDER BY`로 DB에서 처리. Java 레이어에서 재정렬 금지.
- N+1 방지를 위해 연관 엔티티는 `LEFT JOIN FETCH`로 함께 로딩.

### 행동 데이터 수집
- `UserEventAspect` (AOP)가 주요 서비스 메서드 호출 시 `UserEvent`를 자동 저장. 서비스 코드에 수집 로직 없음.

## 현재 진행 상황

### 완료
- 바텐더 취향 카드 API (`GET /api/users/me/taste-card`) — main 머지 완료

### 다음 작업
- 개인 술 순위 API — 별점 기반 자동 정렬 방식, 설계부터 시작
- LLM 추천 Phase 2 — 사용자 프로파일 빌더 (이벤트 집계 → 취향 점수화)

## PR 작성 규칙

- PR description의 Changes 항목은 실제 메서드/클래스 이름을 정확히 기재.
- 설계 결정 사항(왜 이렇게 구현했는지)은 반드시 PR description에 포함. 리뷰어가 코드를 보기 전에 의도를 파악할 수 있어야 함.

## Coding Conventions

- **DTO는 `record`로 작성.** `@Getter` + `@AllArgsConstructor` 조합 금지.
  - 단, `@Builder`가 필요하거나 복잡한 `from()` 정적 팩토리가 있는 응답 DTO는 `class` + Lombok 허용 (예: `NoteResponse`).
- 엔티티(`@Entity`)는 Lombok 사용 (`@Getter`, `@Builder` 등).
- 새 `ErrorCode`는 기존 enum에 추가. 별도 예외 클래스 만들지 않음.
- Swagger 문서: 컨트롤러에 `@Operation(summary, description)` + `@Tag` 필수.