앞으로 이 내용을 기반으로 작업해줘.
반드시 COLLABORATION.md와 FEATURES.md, LEARNING.md도 함께 읽고 시작해줘.
변경/개선 사항의 이유는 CHANGELOG.md에 기록되어 있어.

---

## 서비스 방향
- 개인 기록 중심 (개인 7 : 소셜 3)
- 웹 서비스
- 인증: 이메일+비밀번호 + 카카오/구글/네이버 소셜 로그인 + JWT

---

## 엔티티 설계 확정

### User
- Soft Delete 방식 (deleted_at 컬럼) 
- 회원탈퇴: ❌ 미구현
    - 탈퇴 시 deleted_at 기록 + 모든 노트 isPublic = false
    - 30일 유예 기간 후 Hard Delete 스케줄러로 완전 삭제
    - 유예 기간 중 계정 복구 가능
    - 탈퇴 시 연관 데이터(좋아요, 태그 등) 유지 → Hard Delete 시 함께 삭제
- 탈퇴 후 30일 유예 기간 중 동일 이메일 재가입 불가 (Hard Delete 후에만 허용)
  - 이유: 신고 누적으로 탈퇴한 유저가 즉시 재가입하면 신고 기록이 무의미해짐. 30일 유예는 실수로 탈퇴한 유저의 복구 기간이지 우회 경로가 되면 안 됨
  - 코드: existsByEmail이 deletedAt 여부 상관없이 이메일 중복 체크 → 의도된 동작
- 소셜 로그인 유저는 password null 허용
- 소셜 로그인 첫 가입 시 닉네임 설정 페이지로 이동 + 실시간 중복 체크
- 로그인 식별자: 이메일 (username 필드 제거됨)
- 닉네임 변경: 허용, 30일 1회 제한 / 프로필 URL은 내부 ID 기반 (nicknameChangedAt 컬럼 추가 필요 — 미구현)
- 프로필 페이지: 미니 포트폴리오 방식
    - 공개 노트 목록
    - 많이 마신 술 카테고리 (예: "위스키 애호가")
    - 평균 별점, 총 노트 수, 즐겨 쓰는 태그 Top 5 등 통계
    - 별도 집계 테이블 없이 기존 데이터 쿼리로 구현

### Alcohol
- name(영문), nameKo(한글) 컬럼 분리
- AlcoholAlias 테이블 별도 (별칭 검색용)
    - 예: "블랙라벨", "JW Black" → "조니워커 블랙라벨"로 매칭
- 술 검색 시 name + nameKo + AlcoholAlias 통합 검색
- DB에 없는 술 → AlcoholRequest 등록 요청 → 관리자 승인 후 노트 작성 가능 (엄격한 방식 확정)
- 데이터 전략:
    - 1단계: SQL로 초기 데이터 미리 삽입 (자주 마시는 술 위주)
    - 2단계(추후): 유저 등록 요청 → 관리자 승인 방식 추가
    - 어드민 페이지는 지금 만들지 않음
- 크라우드소싱 방식 확정 (추후 구현 시):
    - 유저가 공식 명칭 + 별칭을 함께 제안, 관리자가 검토 후 승인
    - 별칭도 유저가 직접 제안 (관리자가 직접 입력하는 방식은 작업량 과다)
    - 이유: 유저가 실제로 쓰는 별칭을 관리자보다 더 잘 알고 있음. 별칭이 많을수록 검색 품질 향상
    - 구현 시 AlcoholRequest 테이블 추가 필요 (name, nameKo, aliases, status: PENDING/APPROVED/REJECTED)
- 술 상세 페이지 도입 검토 중 (친구와 상의 필요)
    - 해당 술의 평균 별점, 다른 유저 노트 목록 등 집약
    - Vivino 방식 참고 — 술 페이지가 핵심 콘텐츠가 됨
    - Discovery 기능(Q3)과도 연결됨

### Note
- alcohol 필드 (@ManyToOne, nullable = false) → DB에 있는 술 필수 선택
- alcoholName 자유입력 제거 — 엄격한 방식으로 변경 확정 (2026-04-09)
  - 이유: 자유입력 허용 시 같은 술이 "조니워커", "JW Black" 등으로 제각각 저장돼 Discovery/통계/술 상세 페이지 기능 불가
  - DB에 없는 술은 AlcoholRequest로 등록 요청 → 승인 후 노트 작성
- title → 필수
- rating → 필수, 5점 만점 (0.5~5.0, 0.5단위) — DECIMAL(2,1), Java 타입 BigDecimal (11회차에 Double → BigDecimal 전환)
- taste, aroma → **Vivino 방식으로 확정** — Note 엔티티에 String 필드 없음, NoteFlavor 중간 테이블로 관리
- pairing, description → 자유 텍스트
- location → 자유 텍스트, 선택
- drankAt → 선택
- isPublic → 공개 "의도(intent)" 플래그. DRAFT 단계에서도 설정 가능, 실제 피드 노출은 `status=PUBLISHED AND isPublic=true` 조건에서만 발생
- NoteStatus: DRAFT(임시저장) / PUBLISHED(발행) 구분 유지
    - DRAFT: 임시저장 상태, isPublic 선택 가능 (네이버 블로그/Medium/Facebook 방식 — 작성 단계에서 공개 범위를 미리 저장)
    - PUBLISHED: 발행 상태, isPublic 자유롭게 전환 가능
    - **DRAFT로 되돌리기(unpublish) 불가** — 발행 후에는 수정/비공개전환/삭제만 허용 (2026-04-11 결정)
    - **`status`와 `isPublic`은 의미가 완전히 다르다** — `status`는 라이프사이클(완성 여부), `isPublic`은 공개 의도. 11회차에 두 필드를 혼동하던 코드(publish 시 isPublic 동기화 누락, DRAFT isPublic=true 금지)를 정리함

### NoteFlavor (새 테이블 추가) ✅ 구현 완료
- Note ↔ FlavorSuggestion 다대다 중간 테이블
- FlavorType: TASTE / AROMA 구분
- uniqueConstraint(note_id, flavor_id, type) — 같은 노트에 같은 맛/향 중복 불가
- 이유: Vivino처럼 선택 목록에서만 입력 → 데이터 일관성 확보, 술 상세 페이지/Discovery 통계 가능

### NoteImage
- 저장 위치: AWS S3
- 업로드 시점: 노트 저장 요청 시 이미지도 같이 서버로 보내서 S3에 업로드 후 저장
- 이미지 장수: 우선 1장으로 운영 (추후 변경 가능, 미확정)

### Like (반응)
- LikeType: CHEERS 단일 타입으로 확정 (Untappd의 Toast와 동일 방식, 술 앱 정체성 반영)
- 노트당 하나만 선택 가능
- WANT("마셔보고 싶다")는 나중에 Alcohol 상세 페이지의 위시리스트 기능으로 분리 예정

### FlavorSuggestion ✅ 구현 완료
- taste/aroma 선택 목록 데이터 (관리자가 등록)
- 공통 목록 하나 (술 카테고리별 분리 안 함)
- NoteFlavor 중간 테이블을 통해 Note와 연결

### Report (신고) ✅ 구현 완료
- 신고 사유: Enum (SPAM, INAPPROPRIATE, FALSE_INFO, OTHER)
- OTHER일 때만 reasonDetail 텍스트 입력
- 처리 상태: PENDING(대기) / RESOLVED(처리 완료)
- 같은 유저가 같은 노트를 중복 신고 불가
- 관리자 수동 처리 방식 (추후 자동화 가능)

---

## 기능 확정

### 검색
- 통합 검색창 하나
- 결과를 탭으로 분리 (태그 / 노트 / 술)
- 검색 대상: 태그, 노트 제목, 노트 내용
- 구현 방식: SQL LIKE 검색으로 시작, 데이터 많아져서 느려지면 MySQL Full-Text Search로 전환 예정

### 태그
- 하이브리드 방식 (기존 태그 추천 + 없으면 직접 입력)
- 입력 중 기존 태그 자동완성으로 표시 + 사용 횟수 함께 표시 (예: #위스키 (234))
- 사용 횟수는 NoteTag 개수 실시간 카운팅 방식 (Tag 테이블 컬럼 추가 없음)
- 노트당 태그 개수 제한 없음 (인스타그램 방식)
- 맛/향(taste, aroma)은 FlavorSuggestion 관리자 목록에서 선택 (Vivino 방식)

### 피드
- 공개 노트 피드
- 정렬: 좋아요 많은 순(기본) + 최신순 선택 가능
- 페이지네이션: 커서 방식 (무한 스크롤, 실시간 피드 중복/누락 방지)
- 신고 기능: 신고 버튼만 우선 구현, DB에 기록 후 관리자 수동 처리 (추후 자동화 가능)

---

## 보류 항목 (지금 구현 안 함)
- 팔로우 / 팔로워
- 댓글
- 알림
- 이메일 인증
- 비밀번호 찾기 / 재설정
- NoteImage 개수 제한 (우선 1장, 미확정)
- 기본 프로필 이미지
- 임시저장 개수 제한 및 목록 구분 방식

---

## 패키지 구조
com.dongjin.tastingnote
├── user/entity/User.java
├── user/entity/Provider.java
├── user/entity/UserRole.java                    ← 13회차 신설
├── user/repository/UserRepository.java
├── user/service/UserService.java
├── user/controller/UserController.java
├── user/dto/SignUpRequest.java
├── user/dto/LoginRequest.java
├── user/dto/TokenResponse.java
├── alcohol/entity/Alcohol.java
├── alcohol/entity/AlcoholAlias.java
├── alcohol/entity/AlcoholCategory.java
├── alcohol/entity/AlcoholRequest.java            ← 13회차 신설
├── alcohol/entity/AlcoholRequestStatus.java      ← 13회차 신설
├── alcohol/repository/AlcoholRepository.java
├── alcohol/repository/AlcoholAliasRepository.java ← 13회차 신설
├── alcohol/repository/AlcoholRequestRepository.java ← 13회차 신설
├── alcohol/dto/AlcoholResponse.java
├── alcohol/dto/AlcoholRequestCreateRequest.java  ← 13회차 신설
├── alcohol/dto/AlcoholRequestResponse.java       ← 13회차 신설
├── alcohol/service/AlcoholService.java
├── alcohol/service/AlcoholRequestService.java    ← 13회차 신설
├── alcohol/controller/AlcoholController.java
├── alcohol/controller/AlcoholRequestController.java      ← 13회차 신설
├── alcohol/controller/AdminAlcoholRequestController.java ← 13회차 신설
├── note/entity/Note.java
├── note/entity/NoteImage.java
├── note/entity/Like.java
├── note/entity/LikeType.java
├── note/entity/NoteStatus.java
├── note/entity/NoteFlavor.java
├── note/entity/FlavorType.java
├── note/repository/NoteRepository.java
├── note/repository/NoteFlavorRepository.java
├── note/repository/NoteImageRepository.java
├── note/service/NoteService.java
├── note/controller/NoteController.java
├── note/dto/NoteBaseRequest.java        ← 12회차 신설 (공통 추상 클래스)
├── note/dto/NoteCreateRequest.java
├── note/dto/NoteUpdateRequest.java
├── note/dto/NoteResponse.java
├── tag/entity/Tag.java
├── tag/entity/NoteTag.java
├── tag/repository/NoteTagRepository.java
├── flavor/entity/FlavorSuggestion.java
├── flavor/repository/FlavorSuggestionRepository.java
├── flavor/service/FlavorSuggestionService.java
├── flavor/controller/FlavorSuggestionController.java
├── report/entity/Report.java
├── report/entity/ReportReason.java
├── report/entity/ReportStatus.java
├── report/repository/ReportRepository.java
├── report/service/ReportService.java
├── report/controller/ReportController.java
├── report/dto/ReportRequest.java
├── feedback/entity/Feedback.java        ← 12회차 신설
├── feedback/entity/FeedbackCategory.java
├── feedback/entity/FeedbackStatus.java
├── feedback/repository/FeedbackRepository.java
├── feedback/service/FeedbackService.java
├── feedback/controller/FeedbackController.java
├── feedback/dto/FeedbackRequest.java
├── common/BaseEntity.java
├── common/resolver/CurrentUserId.java              ← 12회차 신설
├── common/resolver/CurrentUserIdArgumentResolver.java
├── common/response/ErrorResponse.java
├── common/exception/GlobalExceptionHandler.java
├── common/notification/NotificationPort.java       ← 12회차 신설 (인터페이스)
├── common/notification/SlackNotificationService.java ← 12회차 rename (NotificationService → SlackNotificationService)
├── common/jwt/JwtTokenProvider.java
├── common/jwt/JwtAuthenticationFilter.java
├── common/config/SecurityConfig.java
├── common/config/WebMvcConfig.java                 ← 12회차 신설
└── common/config/SwaggerConfig.java

## 공통
- 모든 엔티티는 BaseEntity 상속 (createdAt, updatedAt)
- @ManyToOne은 fetch = FetchType.LAZY
- Lombok 사용
- @NoArgsConstructor(access = AccessLevel.PROTECTED)

## 개발 규칙 — 새 기능 구현 시 체크리스트

### 프론트엔드 개발 시작 전 반드시 추가할 것
- **CORS 설정** — SecurityConfig에 추가 필요
  - 이유: 브라우저는 프론트(예: localhost:3000)와 백엔드(예: localhost:8080) 주소가 다르면 API 호출을 막음
  - 추가 위치: `SecurityConfig.filterChain()` 내부에 `.cors(...)` 설정



### ErrorCode 추가 규칙
새 기능을 구현할 때 필요한 ErrorCode를 함께 추가한다. 아래 목록을 보고 해당 기능 구현 시점에 반영할 것:

| 기능 | 추가할 ErrorCode | HTTP |
|------|----------------|------|
| Like | `ALREADY_LIKED` | 409 |
| NoteImage (S3) | `IMAGE_UPLOAD_FAILED` | 500 |

> ErrorCode는 비즈니스 로직 오류만 관리. 프레임워크 예외(타입 불일치 등)는 GlobalExceptionHandler에서 직접 처리.

### 노트 삭제 시 연관 데이터 삭제 순서 (FK 제약 때문에 순서 중요)
```
Report → NoteImage → NoteFlavor → NoteTag → Note
```
> NoteImage S3 업로드 구현 시 S3에서도 파일 삭제 로직 추가 필요.
> **Like 기능 구현 시 반드시 추가**: Like(note_like)도 note_id FK 있음 → LikeService 구현 후 deleteNote()에 `likeRepository.deleteAllByNoteId(noteId)` 추가 필요. 순서: Report → NoteImage → NoteFlavor → NoteTag → Like → Note

### 알려진 설계 주의사항

| # | 항목 | 내용 |
|---|------|------|
| 1 | rating 0.5 단위 검증 | @Min(1)/@Max(5)만 있고 0.5 단위 검증 없음 → 1차 수정(2026-04-10, Double 기반) → 부동소수점 오차로 3.5001 같은 값 통과 가능함을 발견 → BigDecimal로 재전환 + 범위 0.5~5.0 조정 (11회차, 2026-04-12) |
| 2 | 빈 키워드 검색 | keyword="" 시 LIKE %% → 전체 반환. 최소 1자 검증 추가 필요 → 수정 완료 (2026-04-10) |
| 3 | AlcoholCategory 한글명 | API 응답에 영문 enum만 반환. categoryKo 필드 추가로 해결 → 수정 완료 (2026-04-10) |
| 4 | 탈퇴 유저 노트 피드 노출 | UserService 탈퇴 구현 시 반드시 모든 노트 isPublic=false 처리 필요 |
| 5 | 탈퇴 후 Access Token 유효 | 탈퇴해도 기존 Access Token 만료(1시간)까지 API 호출 가능. 탈퇴 기능 구현 시 Access Token 만료 시간 단축(15~30분) 검토 필요 |
| 8 | 탈퇴 시 닉네임 처리 필수 | nickname 컬럼에 DB UNIQUE 제약 있음. 탈퇴 유저 닉네임을 그대로 두면 다른 사람이 같은 닉네임 가입 시도 시 500 에러. 탈퇴 구현 시 반드시 nickname을 고유값으로 변경 필요 (예: "동진_deleted_42") |
| 6 | 카테고리 단일 매칭 | AlcoholCategory.findByNameKo()가 첫 번째 매칭 카테고리만 반환. "주" 검색 시 SOJU만 매칭. 복수 카테고리 매칭은 추후 개선 |
| 7 | 로그인 브루트포스 방어 없음 | Rate limiting 미구현. 서비스 오픈 전 Nginx 또는 AWS WAF 레벨에서 처리 예정 |
| 9 | N+1 SELECT 쿼리 (의도적 보류) | getMyNotes/getPublicNotes에서 노트마다 alcohol/flavor를 개별 쿼리로 조회. 면접 스토리 목적으로 의도적으로 LAZY → @EntityGraph 전환 경험을 남겨둠. FEATURES.md 면접 스토리 #1 참고 |
| 10 | deleteAllByNoteId 삭제 N+1 | ✅ 수정 완료 (10회차) — @Modifying(clearAutomatically=true) @Query 방식으로 전환 |
| 11 | 목록 조회 정렬 기준 없음 | ✅ 수정 완료 (10회차) — OrderByCreatedAtDesc 추가 |
| 12 | S3 분산 일관성 (보류) | S3 업로드/삭제와 DB는 원자적이지 않아 간헐적 고아 파일 발생 가능. 현재 허용. 추후 S3 Lifecycle 정책(미참조 파일 자동 삭제)으로 보완 예정 |

---

## 구현 현황

### 완료
- AlcoholRequest v2 리팩터링 (feature/alcohol-request-v2, 18회차)
  - `AlcoholRequestType` enum 신설 (NEW / ALIAS)
  - `AlcoholRequest` 엔티티: `type`(NOT NULL), `targetAlcohol` 추가; `name`, `category` nullable 변경
  - 유저 API: `POST /api/alcohol-requests` (name OR nameKo 하나 이상 필수), `POST /api/alcohol-requests/{alcoholId}/alias` 신설
  - 관리자 API: `approve-alias` 추가, `merge` 제거, `GET` type 필터 추가
  - `AlcoholAliasCreateRequest` DTO 신설
  - `AlcoholRequestResponse`: type, targetAlcoholId, targetAlcoholName 추가
  - prod DB 기존 레코드 처리: `UPDATE alcohol_request SET type = 'NEW' WHERE type IS NULL;` 필요
- 버그 수정 9건 (fix/17th-session-bugfix, 17회차)
  - C1 테스트 오타, C2 updateNote 이미지 유실, C3 헤더 누락 500, C4 카테고리 substring 과잉매칭, C5 nameKo 중복 체크 누락
  - H1 빈 파일 필터 순서, H2 다른 유저 PENDING 중복 허용, H3 S3 delete 예외 미처리, M1 중복 import
- 노트 이미지 S3 업로드/삭제 구현 (feature/note-image-s3, 17회차)
  - S3Port 인터페이스 + S3Service 구현체 (upload/delete)
  - NoteController createNote/updateNote multipart/form-data 전환
  - 이미지 최대 3장, 파일당 5MB, 전체 15MB 제한
  - 노트 수정 시 이미지 전달하면 전부 교체, 미전달 시 기존 유지
  - 노트 삭제 시 S3 파일도 함께 삭제
  - NoteResponse에 imageUrls 필드 추가
  - AWS IAM 사용자 생성 + S3 버킷 설정 완료
- 술 초기 데이터 삽입 (`data.sql`, 170개 술 + 90개 AlcoholAlias, 15회차)
- 공통 패턴 정리 (refactor/common-pattern-cleanup, 16회차)
  - AlcoholRequestService: `validateNoDuplicateName()` 헬퍼 추출, `saveAliases()` saveAll 전환
  - AlcoholRequestController / AdminAlcoholRequestController: void 응답 200 → 204
- 엔티티 전체 (User, Alcohol, AlcoholAlias, AlcoholCategory, Note, NoteImage, Like, Tag, NoteTag)
- Note CRUD (NoteService, NoteController, DTO 3종)
- JWT 인증 기반 구조 (JwtTokenProvider, JwtAuthenticationFilter)
- User 인증 API (회원가입, 로그인, 로그아웃, 토큰 재발급)
- SecurityConfig JWT 필터 등록 및 URL 인증 정책
- 환경별 설정 분리 (application-local.yaml / application-prod.yaml)
- 브랜치 전략 도입 (main + feature/*)
- 공통 ApiResponse<T> 적용 (모든 컨트롤러)
- NoteController userId → JWT에서 추출 완료
- 신고(Report) 기능 (ReportEntity, ReportService, ReportController)
- Swagger @Tag, @Operation, @SecurityRequirement 추가 (컨트롤러 5개: UserController, NoteController, ReportController, AlcoholController, FlavorSuggestionController)
- SwaggerConfig JWT 보안 스킴 등록 (Authorize 버튼)
- Note 엔티티 rating 컬럼 버그 수정 (precision/scale → columnDefinition)
- feature/jwt-auth → main 머지 완료 (2026-04-02)
- GitHub Actions CI/CD 배포 성공 확인
- 개발 환경: 노트북 → 데스크탑 전환 완료(대부분 노트북으로 작업 후 데스크탑으로 가져올 예정)
- feature/flavor-suggestion → main PR 머지 완료 (PR #1, 2026-04-05, GitHub 웹에서 첫 PR)
- FlavorSuggestion 엔티티/Repository/Service/Controller 구현 완료 (feature/flavor-suggestion, 2026-04-03)
- feature/alcohol-api 브랜치 생성 완료 (feature/flavor-suggestion에서 파생)
- AlcoholRepository/Service/Controller 구현 완료 (feature/alcohol-api, 2026-04-03)
- GlobalExceptionHandler 구현 완료 (feature/alcohol-api, 2026-04-03)
- 보안 이슈 수정 완료 (feature/alcohol-api, 2026-04-03)
  - 노트 수정/삭제/발행/되돌리기에 본인 확인 추가
  - 비공개/DRAFT 노트 타인 조회 차단
  - 자기 노트 신고 방지
  - SecurityConfig 공개 피드(/api/notes/public) 비로그인 허용 추가
  - DRAFT 상태 노트 isPublic=true 설정 차단 (NoteService updateNote)
- NoteCreateRequest rating @NotNull 필수 검증 추가 (feature/alcohol-api, 2026-04-03)
- feature/error-handling → main PR 머지 완료 (PR #2, 2026-04-07)
  - ErrorCode enum (HTTP 상태코드 + 에러코드 + 메시지 한 곳에서 관리)
  - BusinessException 커스텀 예외 클래스
  - ErrorResponse record DTO (success, errorCode, message)
  - GlobalExceptionHandler 업데이트 (BusinessException 처리 추가)
  - UserService, NoteService, ReportService: IllegalArgumentException → BusinessException 교체
- feature/error-notification → main PR 머지 완료 (PR #3, 2026-04-07)
  - NotificationService — 500 에러 발생 시 Slack Webhook 알림 전송
  - AppConfig — RestTemplate Bean 등록
  - SLACK_WEBHOOK_URL 환경변수로 관리
- Note taste/aroma Vivino 방식으로 재설계 (feature/note-flavor-redesign, 2026-04-05)
  - Note 엔티티 taste/aroma String 필드 제거
  - NoteFlavor 중간 테이블 추가 (FlavorType: TASTE/AROMA)
  - NoteCreateRequest/UpdateRequest: tasteIds, aromaIds(List<Long>)로 변경
  - NoteResponse: tastes, aromas(List<String>)로 변경
  - NoteService: saveFlavors(), 수정 시 기존 삭제 후 재저장
- feature/note-flavor-redesign, 2026-04-09
  - AlcoholService.getById() IllegalArgumentException → BusinessException 수정
  - NoteCreateRequest/UpdateRequest @NotNull 추가 (tasteIds, aromaIds, isPublic, rating)
    - tasteIds/aromaIds: null 명시 입력 시 NPE 방지 (빈 배열은 허용)
    - isPublic: nullable=false 컬럼이므로 null 저장 방지
    - rating: 설계상 필수 항목인데 UpdateRequest에만 누락됐던 것 수정
  - Note 엔티티 alcoholName 자유입력 필드 제거, alcohol nullable=false로 변경 (엄격한 방식 확정)
  - NoteCreateRequest alcoholName 제거, alcoholId @NotNull 필수화
  - NoteUpdateRequest alcoholId @NotNull 추가 (수정 시 술 변경도 가능)
  - Note.update()에 Alcohol 파라미터 추가
  - NoteResponse: alcoholName(자유입력) → alcoholName(공식 영문) + alcoholNameKo(공식 한글)
  - ReportRepository.deleteAllByNoteId() 추가
  - NoteService.deleteNote(): Report → NoteFlavor → Note 순서로 삭제 (FK 오류 수정)
- feature/alcohol-category-search → main PR 머지 완료 (PR #5, 2026-04-10)
- feature/alcohol-category-search (2026-04-10)
  - AlcoholCategory enum에 한글명 추가 (nameKo 필드, findByNameKo 메서드)
  - AlcoholService.search(): 카테고리 한글명 매칭 추가 (예: "위스키" → WHISKEY 전체 반환)
  - AlcoholResponse에 categoryKo 필드 추가 (프론트 한글 표시용)
  - NoteTagRepository 생성 (deleteAllByNoteId)
  - NoteService.deleteNote(): NoteTag 삭제 추가 (Report → NoteImage → NoteFlavor → NoteTag → Note)
  - AlcoholController: 빈 키워드 검색 방지 (@Validated + @Size(min=1))
  - NoteService: rating 0.5 단위 검증 추가 (1.3점 등 잘못된 값 방지)
  - SecurityConfig: 비로그인 노트 상세 조회 허용 (RegexRequestMatcher로 /api/notes/{숫자} GET만 허용)
  - NoteController.getNote(): userId 선택적 추출 (비로그인 시 null 처리)
  - FlavorSuggestionResponse: id 포함 응답으로 변경 (노트 작성 시 flavorId 필요)
  - 보안 취약점 수정 (2026-04-10)
    - ReportService: 비공개/DRAFT 노트 신고 차단 (FORBIDDEN_ACCESS)
    - ReportService: reason=OTHER 시 reasonDetail 필수 검증
    - UserService.reissue(): 탈퇴 유저(deletedAt != null) 토큰 재발급 차단
    - SignUpRequest: 닉네임 공백 불가 (@Pattern "^\S+$")
    - SignUpRequest: 비밀번호 영문+숫자 필수 (@Pattern)
    - NoteController: createNote Swagger 설명 수정 (status 필드 오해 제거)
  - 데이터 일관성 수정 (2026-04-10)
    - Note.saveDraft(): isPublic false로 초기화 (unpublish 시 공개 상태 유지 버그 수정)
    - NoteService.createNote(): isPublic 항상 false 고정 (DRAFT+isPublic=true 불일치 방지)
    - NoteService.saveFlavors(): distinct() 추가 (중복 flavorId 방어)
    - NoteCreateRequest: isPublic 필드 제거 (생성 시 의미 없는 필드 혼란 방지)
  - 입력 검증 추가 (2026-04-10)
    - NoteCreateRequest/UpdateRequest: title, location @Size(max=100)
    - AlcoholController: keyword @NotBlank 추가 (공백 키워드 전체 반환 방지)
    - AlcoholService.search(): keyword.trim() 적용 (앞뒤 공백 처리)
    - UserRepository: existsByNicknameAndDeletedAtIsNull 사용 (탈퇴 유저 닉네임 즉시 해제)
    - UserService.signUp()/login(): email.toLowerCase() 정규화 (대소문자 이메일 통일)
    - ReportRequest.reasonDetail: @Size(max=500) 추가
  - 응답 구조 개선 (2026-04-10)
    - NoteResponse tastes/aromas: List<String> → List<FlavorItem>(id+name) 변경 (수정 화면 복원 지원)
    - Note.pairing: @Column(columnDefinition = "TEXT") 추가 (VARCHAR 255 제한 제거)
  - 추가 개선 (2026-04-10)
    - AlcoholCategory.findByName(): 영문 카테고리명 매칭 추가 (whiskey, wine 등 영문 검색 가능)
    - CustomAuthenticationEntryPoint: 미인증 요청 시 빈 응답 대신 JSON 에러 반환
    - ErrorCode.UNAUTHORIZED 추가 ("로그인이 필요합니다")
- fix/10th-session-cleanup (2026-04-11) — 10회차 전체 점검 및 버그 수정
  - NoteRepository 목록 조회 메서드 전체 OrderByCreatedAtDesc 추가 (정렬 보장)
  - SecurityConfig X-Frame-Options 비활성화 설정 제거 (Clickjacking 보안 취약점)
  - NoteFlavorRepository, NoteImageRepository, NoteTagRepository, ReportRepository: deleteAllByNoteId @Modifying @Query 방식으로 전환 (삭제 N+1 해결)
  - NoteService.saveFlavors(): findAllById + saveAll 벌크 방식으로 전환 + 존재하지 않는 ID 검증 추가
  - NoteController/NoteService: unpublishNote 엔드포인트 제거 (발행 후 DRAFT 복귀 불필요)
  - Note 엔티티: saveDraft() 메서드 제거
  - RefreshTokenRepository.deleteByUser(): @Modifying(clearAutomatically=true) @Query 방식으로 전환 (derived delete N+1 해결)
- fix/11th-session-cleanup (2026-04-12) — 11회차 전체 점검 및 버그 수정
  - Note 작성/발행 플로우 isPublic 동기화 버그 수정 (`7cf8537`)
    - NoteCreateRequest에 isPublic 필드 추가 — 작성 단계에서 공개 여부 미리 선택(네이버 블로그/Medium 방식)
    - NoteService.createNote: `.isPublic(false)` 하드코딩 제거 → request 값 사용
    - NoteService.updateNote: DRAFT 상태에서 isPublic=true 금지하던 의미 없는 제약 제거
    - 기존에는 publishNote가 status만 PUBLISHED로 바꾸고 isPublic은 false로 남겨두어, 공개 피드에 노트가 노출되지 않는 심각한 버그가 있었음
  - rating 타입을 Double → BigDecimal로 전환하여 정밀도 보장 (`9b13e50`)
    - Note 엔티티, DTO(Create/Update/Response) rating 필드 전환
    - validateRating: `Math.round(rating*10) % 5` → `rating.remainder(new BigDecimal("0.5"))` 방식으로 재작성
    - @Min/@Max → @DecimalMin/@DecimalMax, 허용 범위 0.5~5.0으로 조정
    - 이유: Double은 부동소수점 오차로 0.5 단위가 아닌 값(예: 3.5001)이 간헐적으로 통과할 수 있었음. DB 컬럼 DECIMAL(2,1)과 Java 타입 불일치 문제도 함께 해소
  - Report 중복 신고 TOCTOU race condition 방어 (`025d105`)
    - Report 엔티티에 `(reporter_id, note_id)` 복합 unique 제약 추가 (`uk_report_reporter_note`)
    - ReportService.report: save() 호출을 try/catch로 감싸 DataIntegrityViolationException → ALREADY_REPORTED 예외 변환
    - 이유: 기존에는 existsBy 체크 후 save하는 TOCTOU 패턴에 DB 제약도 없어, 동시 요청 시 중복 신고가 조용히 쌓여 신고 수 부풀리기 어뷰징 가능했음. DB 제약을 최종 방어선으로 두고 서비스는 예외 매핑으로 409 응답 일관성을 유지하는 이중 구조 적용
  - Refresh Token Rotation + Reuse Detection 적용 (OAuth 2.0 Security BCP) (`c8fb0fe`)
    - RefreshToken 엔티티에 `revoked` 필드 + `revoke()` 메서드 추가
    - UserService.reissue: hard delete → `revoke()` 처리 (재사용 감지용 흔적)
    - reissue 시 이미 revoked된 토큰이 재사용되면 탈취 의심으로 판단하여 해당 유저의 모든 RefreshToken 삭제 → 공격자 세션까지 즉시 차단
    - login/logout은 기존대로 deleteByUser 유지 (clean slate)
    - issueTokens 내부의 deleteByUser 호출 제거, 호출자가 명시적으로 정리 (책임 분리)
    - 이유: 기존에는 공격자가 탈취한 RT로 먼저 reissue하면 정상 유저의 재발급 요청이 조용히 실패할 뿐 탈취 탐지가 불가능했음. OAuth 2.0 Security BCP의 Refresh Token Rotation with Reuse Detection 패턴을 적용
- refactor/12th-session-cleanup (2026-04-15) — 12회차 코드 품질 개선 + 피드백 인프라
  - RT Stateless 전환: RefreshToken 엔티티/Repository 삭제, UserService DB 로직 제거
    - 로그인 DB 3회 → 1회, 재발급 DB 2회 → 0회
    - JwtTokenProvider.validateAndGetUserIdFromRefreshToken() 추가 (만료 vs 위변조 구분)
    - Reuse Detection 제거됨 — 필요 시 추후 Redis 블랙리스트로 대체 예정
  - @CurrentUserId ArgumentResolver 도입 — SecurityContextHolder 직접 접근 7곳 제거
    - common/resolver/CurrentUserId.java + CurrentUserIdArgumentResolver.java 신설
    - common/config/WebMvcConfig.java 신설
  - ResponseEntity + ApiResponse 이중 래핑 제거 — ApiResponse.java 삭제
    - 모든 컨트롤러 반환 타입 ResponseEntity<T>로 통일
    - void 응답 → ResponseEntity.noContent().build() (204)
  - NotificationPort 인터페이스 분리 — 외부 시스템 연결부 추상화
    - NotificationService.java → SlackNotificationService implements NotificationPort
    - GlobalExceptionHandler: NotificationPort 주입
  - GlobalExceptionHandler: 4xx도 Slack 알림 전송
    - 5xx: 🚨 + 스택트레이스(com.dongjin 패키지 최대 8줄)
    - 4xx: ⚠️ + 메서드/URL/에러/메시지
  - Slack 환경변수 분리: SLACK_WEBHOOK_URL → SLACK_ERROR_WEBHOOK_URL + SLACK_FEEDBACK_WEBHOOK_URL
  - Feedback 엔티티/API 신설 — POST /api/feedbacks (로그인 불필요)
    - FeedbackCategory: BUG / FEEDBACK / SUGGESTION
    - FeedbackStatus: OPEN / IN_PROGRESS / RESOLVED
    - 피드백 제출 시 SLACK_FEEDBACK_WEBHOOK_URL 채널로 즉시 알림
  - NoteService.findNoteAndValidateOwner() 헬퍼 추출 (updateNote/publishNote/deleteNote)
  - NoteBaseRequest 추상 클래스 신설 — NoteCreateRequest / NoteUpdateRequest 상속
- feature/alcohol-request → main PR 머지 완료 (PR #8, 2026-04-15) — 13회차
  - UserRole(USER/ADMIN) 신설, JWT role 클레임, /api/admin/** ADMIN 전용 보호 (RBAC)
  - AlcoholRequestStatus(PENDING/APPROVED/MERGED/REJECTED) 열거형
  - AlcoholRequest 엔티티 (@ElementCollection aliases 포함)
  - AlcoholRequestService: request / getRequests / approve / merge / reject
  - AlcoholRequestController(유저), AdminAlcoholRequestController(관리자) 분리
  - AccessDeniedHandler: USER가 관리자 API 접근 시 JSON 403 반환
  - 승인/병합 시 name, nameKo도 AlcoholAlias에 자동 추가 (검색 품질 향상)
  - 중복 요청 체크 alias 포함 강화, 거절 사유 필드 추가
  - AlcoholRequest.aliases LazyInitializationException 수정 (FetchType.EAGER)
  - Swagger 서버 URL 환경변수 관리 (SWAGGER_SERVER_URL, Mixed Content 해결)

### 미완성 (다음 순서)
> 작업 시작 전 반드시 새 브랜치 먼저 만들기: `git checkout -b feature/브랜치명`

1. ~~FlavorSuggestion 엔티티 생성~~ ✅ 완료
2. ~~AlcoholService / AlcoholController~~ ✅ 완료 (feature/alcohol-api, 2026-04-03)
   - GET /api/alcohols/search?keyword= (name + nameKo + alias 통합 검색)
   - GET /api/alcohols?category= (카테고리별 목록)
   - GET /api/alcohols/{id} (단건 조회)
   - SecurityConfig에 /api/alcohols/**, /h2-console/** permitAll 추가
3. ~~AlcoholRequest (크라우드소싱)~~ ✅ 완료 (13회차, PR #8, 2026-04-15)
   - 술 데이터 품질이 서비스의 기반이므로 Tag/Like보다 우선
   - alcoholName 자유입력 제거로 DB에 없는 술은 반드시 AlcoholRequest를 거쳐야 함 (엄격한 방식 확정)
   - 초기 술 DB SQL 삽입(A안) + 크라우드소싱(B안) 조합으로 진행
   - AlcoholRequest 엔티티 설계:
     - name, nameKo, aliases, status, requestedBy(User), mergedToAlcoholId
     - status: PENDING / APPROVED / MERGED / REJECTED
   - 관리자 처리 액션 3가지:
     - **승인 (신규 등록)**: 새로운 술 → Alcohol + Alias 생성
     - **별칭으로 병합**: 이미 있는 술 → 기존 Alcohol에 alias만 추가 (mergedToAlcoholId 기록)
     - **거절**: 장난/중복 등
   - API 5개:
     - POST /api/alcohol-requests — 유저: 술 등록 요청
     - POST /api/admin/alcohol-requests/{id}/approve — 관리자: 승인
     - POST /api/admin/alcohol-requests/{id}/merge?alcoholId= — 관리자: 별칭 병합
     - POST /api/admin/alcohol-requests/{id}/reject — 관리자: 거절
     - GET /api/admin/alcohol-requests?status=PENDING — 관리자: 대기 목록 조회
   - 초반 운영: 어드민 페이지 없이 Swagger에서 관리자 API 호출
   - 나중에 어드민 페이지 만들 때 프론트만 얹으면 됨 (백엔드 API 변경 불필요)
   - 목록 조회 응답에 similarAlcohols 포함 (기존 DB에서 유사 술 자동 검색 → 병합 판단 도움)
---
3-1. ~~**AlcoholRequest 리팩터링**~~ ✅ 완료 (18회차, feature/alcohol-request-v2)
   - **배경**: 현재 구조는 name(영문 필수) + nameKo(선택)인데, 유저가 영어 명칭을 모를 수도 있고 한국어 명칭만 알 수도 있음. 불필요하게 까다로움.
   - **변경 방향**: 신규 등록 / 별칭 추가를 같은 테이블(AlcoholRequest)에서 type으로 구분
   
   ### 엔티티 변경
   - `AlcoholRequest`에 `type` 필드 추가: `NEW` / `ALIAS`
   - `AlcoholRequest`에 `targetAlcohol` 필드 추가 (`@ManyToOne`, ALIAS 요청 시 대상 술)
   - `name` 필드: 더 이상 영문 강제 아님 (영문 OR 한글 자유)
   - `nameKo` 필드: 선택. name/nameKo 중 **하나 이상** 필수 (커스텀 유효성 검사 추가)
   
   ### 유저 API
   - `POST /api/alcohol-requests` — 신규 술 등록 요청 (type=NEW)
     - name OR nameKo 중 하나 이상 필수 (둘 다 입력 가능)
     - aliases 선택 (신규 등록 시 별칭도 함께 요청 가능 — 기존 유지)
     - reason, category 기존과 동일
   - `POST /api/alcohol-requests/{alcoholId}/alias` — 기존 술에 별칭 추가 요청 (type=ALIAS)
     - alias(별칭 하나 이상) + 선택적 reason
     - 관리자 승인 필요 (이상한 별칭 방지)
   
   ### 관리자 API
   - 기존: approve / merge / reject (신규 요청용)
   - 추가: `POST /api/admin/alcohol-requests/{id}/approve-alias` — 별칭 요청 승인 (AlcoholAlias에 추가)
   - `POST /api/admin/alcohol-requests/{id}/reject` — 신규/별칭 요청 모두 공용
   - `GET /api/admin/alcohol-requests?status=PENDING&type=NEW|ALIAS` — type 필터 추가
   
   ### 중복 검증 정리
   - nameKo null 허용으로 `existsByNameKoIgnoreCase(nameKo)` null 가드 추가 필요
   - 기존 merge 엔드포인트 제거 가능 (별칭 추가 요청이 유저 주도 방식으로 대체)

---
> **출시 로드맵 확정 (2026-04-16)**
> 4번 → 5번 → 6번 → 출시. Tag/Like/술 상세 페이지는 출시 후.

4. ~~**술 초기 데이터 삽입**~~ ✅ 완료 (15회차, 2026-04-16)
   - 방법: `src/main/resources/data.sql` (로컬 H2 앱 시작 시 자동 실행) + prod는 SSH로 한 번 실행
     ```bash
     scp data.sql ubuntu@13.124.79.235:~/
     mysql -u root -p tastingnote < data.sql
     ```
   - 목표 수량: 약 150~180개 + AlcoholAlias 포함
     - 위스키 60~70개 (테이스팅 노트 앱 핵심 타겟)
     - 와인 30~40개
     - 맥주 25~30개
     - 소주 10개 / 막걸리 10개 / 사케 10~15개
     - 보드카/진/럼/테킬라/브랜디 각 5~8개
   - 나중에 Flyway 도입 시 `data.sql` → `V2__seed_data.sql`로 이름만 바꾸면 됨

5. ~~**NoteImage S3 업로드**~~ ✅ 완료 (17회차, 2026-04-17, PR #10)

5-1. ~~**AlcoholRequest v2 리팩터링**~~ ✅ 완료 (18회차, 2026-04-19)

6. **소셜 로그인 (OAuth2)** → 완료 후 출시 ← 다음 작업
   - 카카오 / 구글 / 네이버

7. **[출시 직전] Flyway 도입 + ddl-auto: validate 전환**
   - 소셜 로그인까지 완료 후 엔티티가 안정된 시점에 진행
   - `ddl-auto: update`는 운영 DB에서 위험 (오타 컬럼이 조용히 생기거나, 불필요한 컬럼 누적)
   - 작업: build.gradle에 flyway 의존성 추가 + V1__init_schema.sql + V2__seed_data.sql 정리 + prod yaml 변경

---
> **출시 후 구현 예정**

8. 술 상세 페이지 API (GET /api/alcohols/{id}/notes|stats|flavors)
   - 친구와 최종 확인 후 진행

9. TagService / TagController
   - **결정 필요**: Tag 엔티티에 `count` 필드가 있으나, context.md 확정 내용은 "NoteTag 개수 실시간 카운팅 방식 (Tag 테이블 컬럼 추가 없음)"
     - A안: count 컬럼 유지 — 추가/삭제 시 +1/-1, 조회 빠르지만 동기화 안 맞을 수 있음
     - B안: count 컬럼 삭제 — NoteTag COUNT 쿼리로 조회, 항상 정확하지만 약간 느림

10. LikeService / LikeController
    - Like 구현 후 deleteNote()에 `likeRepository.deleteAllByNoteId(noteId)` 추가 필수
    - Tag, Like, 피드 API 완성 후 N+1 문제 해결 (@EntityGraph 적용)

11. ~~RefreshToken 정리 스케줄러~~ — 12회차에서 RT Stateless 전환으로 불필요해짐

---

## 인프라
- 서버: AWS Lightsail (13.124.79.235)
- CI/CD: GitHub Actions → SSH → systemctl restart tastingnote
- Swagger: https://myfirstsbproject.shop/swagger-ui/index.html
- 로컬: H2 인메모리 DB (application-local.yaml)
- 서버: MySQL (application-prod.yaml, 환경변수로 주입)
