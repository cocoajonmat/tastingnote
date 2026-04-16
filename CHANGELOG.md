# 변경/개선 이유 기록

코드를 왜 이렇게 바꿨는지를 기록하는 파일.
context.md 완료 섹션은 "무엇을 했는지"만 기록하고,
이유가 필요한 결정은 여기에 남긴다.

---

## 2026-04-16 — 노트 수정 버그 수정 (14회차)

### Fixed
- `NoteService.updateNote()`: 수정 내용이 DB에 반영되지 않는 버그 수정
  - 원인: `noteFlavorRepository.deleteAllByNoteId()`가 `@Modifying(clearAutomatically=true)`로 선언되어 있어, 실행 후 영속성 컨텍스트(1차 캐시)가 초기화됨
  - 이로 인해 `note.update()`로 변경한 엔티티가 detached 상태가 되어 트랜잭션 커밋 시 dirty checking 대상에서 제외 → DB에 쓰이지 않음
  - PATCH 응답은 in-memory 객체 기준으로 반환되어 정상처럼 보이지만 실제 DB는 구버전 그대로
  - 해결: `noteRepository.save(note)` 호출을 `deleteAllByNoteId()` 전에 명시적으로 추가하여 영속성 컨텍스트 초기화 전에 flush 보장

---

## 2026-04-16 — AlcoholRequest 개선 및 버그 수정 (13회차 후속)

### Added
- `AccessDeniedHandler` 신설 — USER 권한으로 `/api/admin/**` 접근 시 빈 응답 대신 JSON 403 반환
- `AdminAlcoholRequestController.reject()`: `rejectReason` 필드 추가 (거절 사유 기록)
- 승인(approve)/병합(merge) 시 요청된 `name`, `nameKo`도 `AlcoholAlias`에 자동 추가 (검색 품질 향상)

### Changed
- 중복 요청 체크 강화 — 기존 alias에 포함된 이름으로 재요청 시도도 중복으로 차단
- Swagger 서버 URL을 환경변수 `SWAGGER_SERVER_URL`로 관리 (로컬/prod 분리, Mixed Content 해결)
  - CI/CD GitHub Actions에서 `SWAGGER_SERVER_URL` 환경변수로 배포 시 덮어쓰기 방지

### Fixed
- `AlcoholRequest.aliases` `LazyInitializationException` 수정
  - `@ElementCollection`의 기본 fetch 타입은 LAZY → 트랜잭션 밖 접근 시 세션 닫혀 예외 발생
  - `@ElementCollection(fetch = FetchType.EAGER)`로 변경
- Swagger `@SecurityRequirement` 누락 엔드포인트에 어노테이션 추가

---

## 2026-04-15 — AlcoholRequest (술 등록 요청) 기능 구현

### Added
- `UserRole` 열거형 (`USER` / `ADMIN`) 신설
- `User.role` 필드 추가 (`@Builder.Default = USER`, DB 컬럼 `role VARCHAR NOT NULL`)
- `JwtTokenProvider`: `generateAccessToken(userId, role)` 시그니처 변경, `getUserRole(token)` 추가
- `JwtAuthenticationFilter`: JWT에서 role 추출 → `ROLE_USER` / `ROLE_ADMIN` GrantedAuthority 설정
- `SecurityConfig`: `.requestMatchers("/api/admin/**").hasRole("ADMIN")` 추가
- `AlcoholRequestStatus` 열거형 (`PENDING` / `APPROVED` / `MERGED` / `REJECTED`)
- `AlcoholRequest` 엔티티 (술 등록 요청, `@ElementCollection` aliases 포함)
- `AlcoholRequestRepository`: `findAllByStatusOrderByCreatedAtDesc`, `existsByRequestedByAndNameIgnoreCase`
- `AlcoholAliasRepository` 신설 (approve/merge 시 alias 저장)
- `AlcoholRequestCreateRequest` DTO: name(필수), nameKo, aliases(max 10), reason(max 500), category(필수)
- `AlcoholRequestResponse` DTO: 관리자용 `similarAlcohols` 포함
- `AlcoholRequestService`: request / getRequests / approve / merge / reject
- `AlcoholRequestController`: `POST /api/alcohol-requests` (로그인 필수)
- `AdminAlcoholRequestController`: `GET|POST /api/admin/alcohol-requests/**` (ADMIN 전용)
- ErrorCode 3개: `ALCOHOL_REQUEST_NOT_FOUND`(404), `DUPLICATE_ALCOHOL_REQUEST`(409), `ALREADY_PROCESSED`(409)

### Changed
- `UserService.issueTokens()`: `generateAccessToken(user.getId(), user.getRole())`로 role 전달

### 결정 배경
- Note의 `alcohol` 필드가 `nullable=false`로 변경된 이후 DB에 없는 술은 노트 작성 불가.
  유저가 등록 요청 → 관리자가 Swagger에서 승인/병합/거절 처리하는 흐름으로 운영.
- MERGED: 이미 DB에 있는 술과 사실상 같은 술일 때, 새 Alcohol을 만들지 않고 기존 Alcohol의 별칭으로 추가.
  요청자가 알고 있는 이름(별칭)을 검색 DB에 추가하는 효과 → 검색 품질 향상.
- 관리자 API는 초반에 어드민 페이지 없이 Swagger에서 직접 호출하는 방식으로 운영.

---

## 2026-04-09 — Note 엔티티 alcoholName 자유입력 필드 제거

**결정**: alcoholName String 필드 제거, alcohol @ManyToOne nullable=false로 변경 (엄격한 방식)

**이유**: 자유입력 허용 시 같은 술이 "조니워커", "JW Black", "조니워커 블랙" 등 제각각 저장됨.
이렇게 되면 "이 술을 마신 사람이 몇 명인지", "이 술의 평균 별점" 같은 기능이 불가능해짐.
Discovery/술 상세 페이지/통계 등 핵심 기능이 처음부터 막히는 구조.

**대안**: DB에 없는 술은 AlcoholRequest로 등록 요청 → 관리자 승인 후 노트 작성 가능.

---

## 2026-04-10 — AlcoholCategory에 한글명 추가

**결정**: AlcoholCategory enum에 nameKo 필드와 findByNameKo() 메서드 추가

**이유**: 검색창에 "위스키"를 입력해도 결과가 없었음.
기존 검색은 name(영문) + nameKo(한글) + alias만 체크하고 카테고리명은 포함하지 않았음.
AlcoholCategory enum이 WHISKEY처럼 영문이라 "위스키"와 매칭되지 않아서 해당 카테고리 전체가 누락됨.

---

## 2026-04-10 — FlavorSuggestion 응답에 id 포함

**결정**: FlavorSuggestionResponse를 name(String)만 반환하던 것에서 id + name으로 변경

**이유**: 노트 작성 시 클라이언트가 맛/향을 선택하면 해당 FlavorSuggestion의 id(tasteIds, aromaIds)를 보내야 함.
id 없이 이름만 반환하면 클라이언트가 노트 저장 요청을 만들 수 없음.

---

## 2026-04-10 — 비로그인 유저 노트 상세 조회 허용

**결정**: SecurityConfig에 RegexRequestMatcher("/api/notes/\\d+", "GET").permitAll() 추가.
NoteController.getNote()에서 userId를 선택적으로 추출 (비로그인 시 null).

**이유**: 서비스의 기본 방향이 공개 피드/노트 조회는 비로그인도 가능해야 함.
기존에는 /api/notes/{noteId} GET도 인증 필요해서 비로그인 유저가 아무것도 볼 수 없었음.
단, /api/notes/my와 충돌하지 않도록 숫자 ID만 허용하는 정규식 사용.

---

## 2026-04-10 — 비공개/DRAFT 노트 신고 차단

**결정**: ReportService에서 note.getStatus() == DRAFT || !note.getIsPublic() 이면 403 반환

**이유**: 비공개 노트나 임시저장 노트는 본인 외에 볼 수 없는 콘텐츠.
신고 기능은 다른 유저가 볼 수 있는 공개 콘텐츠에 대해서만 의미 있음.
비공개 노트를 신고할 수 있으면 노트 존재 여부가 간접적으로 노출되는 보안 문제도 있음.

---

## 2026-04-10 — reason=OTHER 시 reasonDetail 필수화

**결정**: ReportService에서 reason == OTHER && reasonDetail이 null/blank이면 INVALID_INPUT 에러

**이유**: OTHER(기타)는 정해진 사유 외의 신고를 위한 항목.
reasonDetail 없이 OTHER를 선택하면 관리자가 어떤 문제인지 파악 불가 → 처리가 불가능한 신고가 DB에 쌓임.

---

## 2026-04-10 — 탈퇴 유저 토큰 재발급 차단

**결정**: UserService.reissue()에서 refreshToken.getUser().getDeletedAt() != null이면 USER_NOT_FOUND 에러

**이유**: Refresh Token은 30일간 유효한데, 탈퇴(deletedAt 기록) 후에도 기존 토큰으로 재발급이 가능했음.
탈퇴한 유저가 Access Token을 계속 받을 수 있으면 탈퇴의 의미가 없음.
로그인(login)은 이미 findByEmailAndDeletedAtIsNull로 차단되어 있지만, reissue는 이메일 조회를 거치지 않아 누락됨.

---

## 2026-04-10 — 닉네임 공백 검증 추가

**결정**: SignUpRequest nickname 필드에 @Pattern(regexp = "^\\S+$") 추가

**이유**: @NotBlank는 전체가 공백인 경우만 막음 (" " → 차단, "닉 네임" → 통과).
닉네임 중간에 공백이 있으면 표시될 때 어색하고, 검색/자동완성 기능에서 예상치 못한 동작 가능.

---

## 2026-04-10 — 비밀번호 복잡도 검증 추가

**결정**: SignUpRequest password 필드에 @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$") 추가

**이유**: 8자 이상 조건(@Size)만으로는 "aaaaaaaa", "12345678" 같은 단순 비밀번호가 허용됨.
최소한 영문자+숫자 조합을 강제해야 브루트포스 공격에 덜 취약함.
특수문자는 강제하지 않음 — 입력 UX와 보안의 균형을 고려한 결정.

---

## 2026-04-10 — Swagger createNote 설명 오류 수정

**결정**: NoteController createNote @Operation description에서 "status 필드로 DRAFT/PUBLISHED 선택 가능" 문구 제거

**이유**: 실제 동작은 생성 시 항상 DRAFT로 저장되며, status 필드는 NoteCreateRequest에 존재하지 않음.
설명과 실제 동작이 다르면 프론트(친구)가 없는 기능을 구현하려고 시간을 낭비할 수 있음.

---

## 2026-04-10 — 이메일 toLowerCase() 처리

**결정**: signUp(), login()에서 이메일을 소문자로 정규화 후 저장/조회

**이유**: DB collation(MySQL utf8mb4_general_ci)이 대소문자를 구분하지 않아 운영에서는 문제없지만,
H2(로컬)는 대소문자를 구분해서 환경 간 동작이 달라짐. 또한 실무 관례상 이메일은 애플리케이션 레벨에서
명시적으로 소문자 정규화하는 것이 DB 설정에 의존하지 않아 안전.

---

## 2026-04-10 — 술 검색 공백 키워드 방어 강화

**결정**: AlcoholController search()에 @NotBlank 추가 (@Size(min=1)과 함께 사용)

**이유**: @Size(min=1)만으로는 " "(공백 1개)를 길이 1로 허용해버림.
공백 키워드로 검색 시 LIKE %  % 쿼리 → 전체 반환으로 빈 검색과 동일한 결과.
@NotBlank는 공백만인 문자열을 차단하므로 함께 써야 완전히 방어됨.

---

## 2026-04-10 — createNote 시 isPublic 항상 false 고정

**결정**: NoteService.createNote()에서 isPublic을 클라이언트 값 무시하고 항상 false로 저장

**이유**: 노트는 생성 시 항상 DRAFT 상태. isPublic은 PUBLISHED 이후에만 의미 있음.
클라이언트가 isPublic=true를 보내면 DRAFT인데 isPublic=true인 불일치 데이터가 생김.
updateNote()에서는 DRAFT+isPublic=true를 차단하는 검증이 있었지만 createNote()에는 없어서 누락됐던 것.

---

## 2026-04-10 — unpublish 시 isPublic false로 초기화

**결정**: Note.saveDraft()에서 isPublic도 false로 함께 변경

**이유**: unpublish는 "임시저장으로 되돌리기"인데 isPublic=true가 유지되면
다시 publish 시 즉시 공개 노출되어 사용자 입장에서 예상치 못한 동작.
isPublic 설정은 PUBLISHED 상태에서 별도로 하는 것이 의미 있음.

---

## 2026-04-10 — 탈퇴 유저 닉네임 즉시 해제

**결정**: UserRepository.existsByNickname() → existsByNicknameAndDeletedAtIsNull()로 변경

**이유**: 이메일은 신고 누적 탈퇴 유저가 즉시 재가입해서 신고 기록을 우회하는 것을 막기 위해 30일 묶어둠.
닉네임은 그런 보안적 이유가 없음. 탈퇴 유저 닉네임을 다른 사람이 못 쓰게 막으면 UX만 불편해짐.
이메일과 닉네임은 성격이 달라서 동일한 규칙을 적용할 필요 없음.

---

## 2026-04-10 — 노트 title/location 길이 제한 추가

**결정**: NoteCreateRequest, NoteUpdateRequest에 title @Size(max=100), location @Size(max=100) 추가

**이유**: 제한 없으면 수천 자짜리 제목이 DB에 저장될 수 있음.
제목은 UI에서 한 줄로 표시되므로 100자면 충분. 장소도 간단한 텍스트이므로 동일 기준 적용.
description과 pairing은 자유 서술 필드(TEXT 컬럼)이므로 제한 없이 유지.

---

## 2026-04-15 — Refresh Token Stateless 전환 (RT DB 저장 제거)

**결정**: RefreshToken 엔티티 + RefreshTokenRepository 삭제. RT를 JWT로만 관리하고 DB에 저장하지 않음.

**이유**: 기존 구조는 로그인 시 DB 3회(User 조회 + RT 삭제 + RT 저장), 재발급 시 DB 2회(RT 조회 + RT revoke+저장) 발생.
RT의 유효성은 JWT 서명과 만료 시간으로 검증 가능 — DB 조회 없이 `JwtTokenProvider`가 처리.
11회차에 추가한 Reuse Detection은 DB 기반이었기에 함께 제거됨.
강제 로그아웃이 필요해지면 추후 Redis 블랙리스트로 추가할 예정.

**추가 변경**: `JwtTokenProvider.validateAndGetUserIdFromRefreshToken()` 추가 — `ExpiredJwtException`과 일반 `JwtException`을 구분해 `EXPIRED_TOKEN` / `INVALID_TOKEN` 에러를 적절히 throw.

---

## 2026-04-15 — @CurrentUserId ArgumentResolver 도입

**결정**: `@CurrentUserId` 어노테이션 + `CurrentUserIdArgumentResolver` 생성. 컨트롤러에서 SecurityContextHolder 직접 접근 제거.

**이유**: `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`이 NoteController 5곳, ReportController 1곳, UserController 1곳 — 총 7곳에서 반복됨. ArgumentResolver로 분리하면 컨트롤러 코드가 선언적으로 바뀌고, 비로그인 시 null 반환 처리도 한 곳에서 관리됨.

---

## 2026-04-15 — ResponseEntity + ApiResponse 이중 래핑 제거

**결정**: 모든 컨트롤러에서 `ResponseEntity<ApiResponse<T>>` → `ResponseEntity<T>` 로 변경. `ApiResponse.java` 삭제.

**이유**: `ResponseEntity`가 HTTP 상태코드를, `ApiResponse`의 `success: true/false`가 동일한 정보를 중복으로 표현하고 있었음. 성공 응답은 데이터만 직접 반환하고, 에러는 `GlobalExceptionHandler`가 `ErrorResponse`를 반환하는 구조로 정리. void 응답은 `204 No Content`로 통일.

---

## 2026-04-15 — NotificationPort 인터페이스 분리 (외부 시스템 연결부)

**결정**: `NotificationPort` 인터페이스 생성. `NotificationService` → `SlackNotificationService implements NotificationPort`로 rename + 구현체화.

**이유**: Slack 웹훅, 추후 S3 등 외부 시스템 연결부는 인터페이스로 분리해야 다른 수단(이메일, Discord 등)으로 교체하거나 테스트 목킹이 쉬워짐. JPA Repository는 이 규모에서 굳이 분리하지 않고 외부 IO 계층에만 적용.

---

## 2026-04-15 — GlobalExceptionHandler 4xx 에러도 Slack 알림 + 스택트레이스

**결정**: 기존에는 500 에러만 Slack 알림 전송. 이제 모든 에러(4xx, 5xx)를 Slack에 전송. 5xx는 스택트레이스(com.dongjin 패키지 필터 최대 8줄) 포함, 4xx는 메서드/URL/에러코드/메시지만.

**이유**: 베타 출시 초기에는 4xx 에러도 모니터링해야 예상치 못한 클라이언트 사용 패턴을 파악할 수 있음. 이모지로 심각도 구분(5xx: 🚨, 4xx: ⚠️) — Slack에서 시각적으로 구분 가능.

---

## 2026-04-15 — Slack 에러 채널 / 피드백 채널 분리

**결정**: 기존 `SLACK_WEBHOOK_URL` 환경변수 하나 → `SLACK_ERROR_WEBHOOK_URL` + `SLACK_FEEDBACK_WEBHOOK_URL` 두 개로 분리.

**이유**: 에러 알림과 유저 피드백이 같은 채널에 오면 중요한 피드백이 에러 메시지에 묻힘. 채널 분리로 담당자별 역할도 구분 가능(예: 에러는 개발자, 피드백은 기획자).

---

## 2026-04-15 — Feedback(버그/피드백) 테이블 신설

**결정**: `feedback` 테이블 신설. `POST /api/feedbacks` 엔드포인트 추가. 로그인 없이도 제출 가능.

**이유**: 완성 전 베타 출시 예정. 유저가 버그나 개선 요청을 보낼 창구가 필요함. 피드백 제출 시 `SLACK_FEEDBACK_WEBHOOK_URL` 채널로 즉시 알림 전송.

---

## 2026-04-15 — NoteService 공통 헬퍼 추출 + NoteBaseRequest 추상 클래스

**결정**: `findNoteAndValidateOwner(noteId, userId)` 헬퍼 추출 (updateNote/publishNote/deleteNote 3곳에 적용). `NoteBaseRequest` 추상 클래스 생성 — `NoteCreateRequest`, `NoteUpdateRequest`가 상속.

추가로 `buildNote(user, alcohol, request)`, `saveFlavorsThenResponse(note, tasteIds, aromaIds)`, `toResponse(note)` 헬퍼 3개 추출. 목록 조회 메서드(getMyNotes, getMyNotesByStatus, getPublicNotes)의 stream lambda도 `this::toResponse`로 단순화.

**이유**: 노트 존재 확인 + 소유자 검증이 3개 메서드에 4줄씩 중복. `NoteCreateRequest`와 `NoteUpdateRequest`는 `isPublic` 검증 여부(기본값 false vs @NotNull)만 다르고 나머지 9개 필드가 동일. Note.builder() 블록과 `findAllByNoteId + NoteResponse.from` 패턴도 각각 2~4곳에서 반복되어 추출. **SRP**: 각 헬퍼는 한 가지 일만 담당. **DRY**: 변경이 필요할 때 한 곳만 수정.

---

## 2026-04-15 — /done 프로젝트 커맨드 생성

**결정**: `.claude/commands/done.md` 생성. `/done` 입력 시 MD 파일 업데이트 → 노션 일지 출력 → git commit 자동 진행.

**이유**: 세션 종료 시 context.md, CHANGELOG.md, FEATURES.md, LEARNING.md 업데이트와 노션 일지 작성을 매번 수동으로 진행하던 것을 커맨드 하나로 자동화.

---

## 2026-04-10 — pairing 컬럼 TEXT로 변경

**결정**: Note.java pairing 필드에 `@Column(columnDefinition = "TEXT")` 추가

**이유**: description과 함께 pairing도 자유 서술 필드. 페어링 음식/안주를 자세히 적을 수 있어야 함.
VARCHAR(255) 기본값으로는 긴 내용 저장 시 잘림. 글자 수 제한은 프론트 UI에서 UX로 처리.

---

## 2026-04-10 — reasonDetail 길이 제한 추가

**결정**: ReportRequest reasonDetail에 `@Size(max=500)` 추가

**이유**: OTHER 사유 입력 시 무제한 텍스트 저장 가능 → DB 저장 공격 벡터.
500자면 충분히 상황 설명 가능. description/pairing과 달리 신고 사유는 간결해야 함.
TEXT가 아닌 VARCHAR(500)으로 DB 컬럼도 자연스럽게 제한됨.

---

## 2026-04-10 — NoteResponse tastes/aromas에 id 포함

**결정**: NoteResponse의 tastes/aromas를 `List<String>`에서 `List<FlavorItem>`(id + name)으로 변경

**이유**: 노트 수정 화면에서 기존에 선택된 맛/향을 다시 표시하려면 FlavorSuggestion의 id가 필요함.
id 없이 name만 있으면 수정 요청 시 tasteIds/aromaIds를 만들 수 없어서 프론트가 기존 선택값을 복원할 수 없음.

---

## 2026-04-11 — 목록 조회 정렬 추가 (최신순)

**결정**: NoteRepository의 findAll* 메서드에 OrderByCreatedAtDesc 추가

**이유**: ORDER BY 없이 조회하면 DB가 내부적으로 편한 순서로 반환함. DB 재시작이나 데이터 변경 시 목록 순서가 달라져 사용자가 새로고침할 때마다 순서가 뒤섞이는 현상 발생 가능. 최신순이 노트 목록/피드의 기본 정렬로 가장 자연스러움.

---

## 2026-04-11 — X-Frame-Options 비활성화 설정 제거

**결정**: SecurityConfig에서 `.headers(headers -> headers.frameOptions(...disable))` 블록 제거

**이유**: H2 콘솔(iframe 구조)을 위해 추가했던 설정인데 모든 환경에 적용되어 있었음. X-Frame-Options가 비활성화되면 내 사이트를 다른 사이트의 보이지 않는 iframe에 삽입해 클릭을 가로채는 Clickjacking 공격에 취약해짐. 제거하면 Spring Security 기본값(X-Frame-Options: DENY)이 적용됨. H2 콘솔은 브라우저 대신 IntelliJ Database 탭 또는 SSH를 통해 접근 가능.

---

## 2026-04-11 — deleteAllByNoteId 벌크 삭제로 전환 (@Modifying)

**결정**: NoteFlavorRepository, NoteImageRepository, NoteTagRepository, ReportRepository의 deleteAllByNoteId()를 @Modifying @Query 방식으로 변경. clearAutomatically = true 포함.

**이유**: Spring Data JPA의 derived delete(메서드명 기반 삭제)는 내부적으로 SELECT 후 각 행을 개별 DELETE하는 방식. 삭제할 데이터가 10개면 쿼리가 11번(SELECT 1 + DELETE 10) 발생. @Modifying @Query는 DELETE FROM ... WHERE ... 단일 쿼리로 처리해 성능 개선. clearAutomatically = true는 삭제 후 JPA 1차 캐시(영속성 컨텍스트)를 자동으로 초기화해서 같은 트랜잭션 내에서 삭제된 데이터가 캐시에서 유령처럼 남아있는 문제를 방지함.

---

## 2026-04-11 — saveFlavors 벌크 조회/저장으로 전환

**결정**: NoteService.saveFlavors()에서 루프 내 개별 findById + save → findAllById + saveAll로 전환

**이유**: 기존 방식은 flavor ID 하나마다 SELECT 1번 + INSERT 1번 발생. taste 5개 + aroma 5개면 쿼리 20번. findAllById로 한 번에 조회하고 saveAll로 한 번에 저장해서 쿼리를 최소화. 존재하지 않는 ID 검증은 조회된 개수와 요청한 ID 수를 비교하는 방식으로 유지.

---

## 2026-04-11 — GlobalExceptionHandler Spring MVC 예외 핸들러 추가

**결정**: `HttpMessageNotReadableException`, `MissingServletRequestParameterException`, `HttpRequestMethodNotSupportedException` 세 가지 핸들러 추가

**이유**: 이 예외들은 클라이언트 실수(잘못된 JSON 타입, 필수 파라미터 누락, 잘못된 HTTP 메서드)인데, 기존에는 처리 핸들러가 없어서 맨 아래 `Exception.class` 핸들러에 걸렸음. 결과적으로 클라이언트에게 500을 반환하고 Slack에 불필요한 서버 에러 알림까지 발송됨. 추가 후에는 적절한 4xx 상태코드와 명확한 에러 메시지 반환.

---

## 2026-04-11 — RefreshTokenRepository.deleteByUser 벌크 삭제로 전환 (@Modifying)

**결정**: `deleteByUser(User user)`를 `@Modifying(clearAutomatically = true) @Query` 방식으로 변경

**이유**: Spring Data JPA derived delete는 내부적으로 대상 엔티티를 먼저 SELECT한 뒤 각각 DELETE를 실행함(N+1). 사용자마다 RefreshToken이 1개이므로 현재는 문제 없지만, 향후 멀티 디바이스 로그인 지원 시 토큰이 여러 개가 될 수 있어 일관성을 위해 JPQL 벌크 삭제로 통일. NoteFlavorRepository 등과 동일한 패턴 적용.

---

## 2026-04-11 — unpublishNote 엔드포인트 제거 결정

**결정**: `/api/notes/{noteId}/unpublish` 엔드포인트 및 관련 서비스/엔티티 메서드 제거

**이유**: 발행 후 DRAFT로 되돌리는 기능은 대부분의 서비스(인스타그램, 브런치 등)에서도 없음.
발행 후 상태 관리는 세 가지로 충분: 수정(updateNote) + 비공개 전환(isPublic=false) + 삭제.
unpublish는 PUBLISHED → DRAFT 상태 역행인데, 이는 "비공개 설정"(isPublic=false, PUBLISHED 유지)과 개념적으로 혼란스럽고 UX도 복잡해짐.
DRAFT 상태에서는 프론트엔드에서 공개/비공개 UI를 애초에 안 보여줄 예정이라 더욱 불필요.

---

## 2026-04-12 — 노트 작성/수정 시 isPublic 동기화 버그 수정

**결정**: NoteCreateRequest에 isPublic 필드 부활, createNote 하드코딩 제거, updateNote에서 DRAFT+isPublic=true 금지 제약 제거

**이유**: 10회차에 "DRAFT 단계에서 isPublic=true는 의미 없는 불일치"라는 판단으로 isPublic 필드를 제거하고 createNote에서 false로 고정, updateNote에서도 DRAFT 공개 전환을 차단했음. 그런데 publishNote()는 status만 PUBLISHED로 바꾸고 isPublic은 건드리지 않기 때문에, 결과적으로 **공개 노트를 만드는 경로 자체가 존재하지 않는** 심각한 버그 상태였음. 공개 피드 쿼리(`status=PUBLISHED AND isPublic=true`)를 통과하는 노트가 생길 수 없었음.

실제 서비스 사례(네이버 블로그/티스토리/벨로그/Medium/Facebook)를 조사한 결과, 대부분 임시저장 단계에서 공개 여부를 **함께 저장**하는 방식이었음. `status`와 `isPublic`은 의미적으로 완전히 다른 필드다:
- `status`: 라이프사이클 (DRAFT = 작성 중, PUBLISHED = 발행됨)
- `isPublic`: 공개 "의도(intent)" — 발행 시점에 피드에 노출할지 여부

DRAFT 상태에서 isPublic=true인 데이터가 생겨도 피드 쿼리가 status 조건을 걸어 막아주므로 노출 위험이 없음. 10회차에 "불일치 방지"라 부르던 방어는 실제로는 과한 제약이었음.

**대안**: `publishNote`에 isPublic 파라미터를 추가(A안)하는 방법도 있었으나, "작성 단계에서 공개 여부를 미리 정한다"는 인스타/블로그식 UX가 사용자 기대에 더 부합하고 프론트 구현도 단순해지므로 B안(DTO에 isPublic 복원) 선택.

---

## 2026-04-12 — rating 타입을 Double → BigDecimal로 전환 (정밀도 보장)

**결정**: Note 엔티티, NoteCreateRequest/UpdateRequest/Response의 rating 필드를 BigDecimal로 변경. validateRating을 `BigDecimal.remainder(new BigDecimal("0.5"))` 기반으로 재작성. @Min/@Max → @DecimalMin/@DecimalMax. 허용 범위 0.5~5.0으로 조정.

**이유**: 10회차에 `Math.round(rating * 10) % 5 != 0` 방식의 0.5 단위 검증을 도입했으나, Double은 IEEE 754 부동소수점 타입이라 정확한 값을 못 담음. `3.5001` 같은 값이 들어오면 `35.00100...` → `Math.round → 35` → `35 % 5 == 0` 으로 통과해 버리는 위험이 간헐적으로 존재했음.

별점처럼 **정확한 단위가 있는 수치**는 애초에 Double로 다루면 안 되는 유형. 금액/수량/점수 같은 값은 실무에서 BigDecimal을 쓰는 게 표준. DB 컬럼이 DECIMAL(2,1)인데 Java 쪽이 Double이어서 타입 불일치로 DB 정밀도의 의미가 희석되던 문제도 함께 해소됨.

범위는 0개 별(0.0)이 "미평가"와 혼동되므로 대부분 서비스가 0.5부터 허용하는 관례를 따라 0.5~5.0으로 조정.

---

## 2026-04-12 — Report 중복 신고 TOCTOU race condition 방어 및 DB unique 제약 추가

**결정**: Report 엔티티에 `(reporter_id, note_id)` 복합 unique 제약(`uk_report_reporter_note`) 추가. ReportService.report에서 save()를 try/catch로 감싸 DataIntegrityViolationException → ALREADY_REPORTED(409) 예외로 변환.

**이유**: 기존 코드는 `existsByReporterIdAndNoteId` 체크 후 `save`하는 전형적인 TOCTOU(Time-Of-Check to Time-Of-Use) 패턴이었고, 엔티티에 unique 제약이 **아예 없었음**. 두 요청이 거의 동시에 도착하면 둘 다 exists 체크를 통과해 양쪽 다 save되고, DB 제약도 없어서 **중복 신고가 조용히 쌓이는** 상황이었음. 같은 유저가 신고 버튼을 연타해 신고 수를 부풀리는 어뷰징이 가능한 상태.

DB unique 제약을 진실의 원천(source of truth)으로 두고, 서비스 레벨 `existsBy` 검증은 정상 케이스에서 빠른 에러 응답을 위한 UX 계층으로 유지. 동시성 충돌이 발생하면 save() 단계에서 `DataIntegrityViolationException`이 터지고, 이를 catch해 사용자에게는 `ALREADY_REPORTED` 비즈니스 예외로 변환해 일관된 409 Conflict를 반환. 앱 레벨 검증(UX) + DB 제약(정합성)의 이중 구조가 실무 정석.

---

## 2026-04-12 — Refresh Token Rotation + Reuse Detection (OAuth 2.0 Security BCP)

**결정**: RefreshToken 엔티티에 `revoked` boolean 필드와 `revoke()` 메서드 추가. UserService.reissue에서 hard delete 대신 revoke 처리로 변경. 이미 revoked된 토큰이 다시 사용되면 탈취 의심으로 판단해 해당 유저의 모든 RefreshToken을 삭제(deleteByUser). login/logout은 기존대로 deleteByUser 유지. issueTokens 내부의 deleteByUser 호출은 제거하고 호출자(login)가 명시적으로 정리하도록 책임 분리.

**이유**: 기존 구조는 reissue 시점에 기존 RT를 즉시 hard delete하고 새 RT를 발급하는 방식이었음. 이 구조에서 공격자가 RT1을 탈취해 먼저 reissue를 호출하면, RT1이 삭제되고 RT2가 공격자에게 발급됨. 이후 정상 유저가 RT1로 reissue를 시도하면 `findByToken(RT1)`이 비어 있어 `INVALID_TOKEN` 에러만 반환되고, **탈취 사실을 전혀 탐지하지 못함**. 공격자는 RT2를 가지고 정상 유저가 재로그인할 때까지 자유롭게 활동 가능.

OAuth 2.0 Security BCP(RFC 6819 + draft-ietf-oauth-security-topics)의 Refresh Token Rotation with Reuse Detection 패턴을 적용:
- reissue 정상 경로: 현재 토큰을 `revoked=true`로 표시(삭제 안 함) + 새 토큰 발급
- revoked된 토큰이 다시 reissue 요청으로 들어오면 **재사용 감지** → 해당 유저의 모든 RT 삭제 → 공격자가 보유한 RT2도 함께 무효화 → 정상 유저는 재로그인 필요하지만 공격자는 완전 차단

issueTokens 내부의 deleteByUser를 빼고 login이 명시적으로 호출하게 바꾼 것은 reissue 정상 경로에서 의도치 않게 revoked 흔적까지 삭제되지 않도록 책임을 분리한 것.

**부수 효과**: revoked 토큰이 DB에 누적됨. 주기적으로 만료/revoked 토큰을 정리하는 스케줄러가 필요. context.md "다음 순서 9번"에 TODO로 기록.

---

## 보류 결정 (추후 처리 예정)

### 탈퇴 후 Access Token 유효 문제
탈퇴해도 기존 Access Token 만료(1시간)까지 API 호출 가능.
완벽히 막으려면 Redis 블랙리스트 필요 — 현재 규모에서 과함.
탈퇴 기능 구현 시 Access Token 만료 시간 단축(15~30분)으로 피해 최소화 예정.

### 로그인 브루트포스 방어
로그인 실패 횟수 제한 없음. Rate limiting은 애플리케이션보다 인프라 레벨이 적합.
서비스 오픈 전 Nginx 설정(`limit_req_zone`) 또는 AWS WAF로 처리 예정.

### Swagger UI prod 비활성화
개발/테스트 단계에서 친구와 prod Swagger로 API 테스트 중이므로 현재 유지.
실제 서비스 오픈 직전에 application-prod.yaml에 springdoc.api-docs.enabled: false 추가 예정.

### ddl-auto: validate 전환 (Flyway 도입)
현재 prod에 `ddl-auto: update` 사용 중. 스키마 자동 변경 위험이 있으나 테스트 단계이므로 유지.
서비스 오픈 전 Flyway(마이그레이션 툴) 도입 후 `validate`로 전환 예정.
Flyway는 SQL 마이그레이션 파일을 버전별로 관리해서 배포 시 자동 실행 — 면접 스토리 가능.

### CORS 설정
프론트엔드 개발 시작 전 SecurityConfig에 `.cors(...)` 설정 추가 필요.
브라우저는 프론트(localhost:3000)와 백엔드(localhost:8080) 도메인이 다르면 API 호출을 차단함.
현재는 백엔드 단독 개발 중이라 무관하지만, 프론트 연동 시작 시 반드시 추가. (context.md 개발 규칙 참고)

### AlcoholCategory.findByName 과도한 매칭 (의도적 보류)
현재 `contains()` 방식으로 카테고리를 매칭하는데, "e" 한 글자 검색 시 BEER/WINE/SAKE 등 여러 카테고리가 매칭될 수 있음.
findByName은 첫 번째 매칭만 반환하므로 실제 사용자 경험에서 큰 문제는 없지만, 의도치 않은 카테고리 데이터가 검색 결과에 포함될 수 있음.
검색 품질 개선(SQL LIKE → Full-Text Search) 전환 시점에 함께 정확도 개선 예정.