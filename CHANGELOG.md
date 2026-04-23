# 변경/개선 이유 기록

코드를 왜 이렇게 바꿨는지를 기록하는 파일.
context.md 완료 섹션은 "무엇을 했는지"만 기록하고,
이유가 필요한 결정은 여기에 남긴다.

---

## 2026-04-23 — 소셜 로그인(OAuth2) 구현 (21회차)

### Added
- `Provider` enum에 `NAVER` 추가
- `UserRepository`: `findByProviderAndProviderId(Provider, String)` 추가
- `ErrorCode`: `SOCIAL_EMAIL_CONFLICT`(409), `SOCIAL_EMAIL_REQUIRED`(400), `UNSUPPORTED_PROVIDER`(400) 추가
- `OAuthClient` 인터페이스: `provider()` + `fetchUserInfo(code, redirectUri)` (S3Port 패턴과 동일)
- `AbstractOAuthClient` 추상 클래스: `fetchAccessToken(tokenUrl, clientId, clientSecret, code, redirectUri)` + `fetchUserInfoMap(userInfoUrl, accessToken)` 공통 로직 추출 — 3개 구현체에서 중복 45줄 제거
- `KakaoOAuthClient`: 카카오 토큰/유저정보 API 호출 (kakao_account.email, profile 파싱)
- `GoogleOAuthClient`: 구글 토큰/유저정보 API 호출 (id, email, name, picture)
- `NaverOAuthClient`: 네이버 토큰/유저정보 API 호출 (response.id, email, nickname, profile_image)
- `OAuthLoginRequest` record: `code`, `redirectUri` (@NotBlank)
- `OAuthLoginResponse` record: `accessToken`, `refreshToken`, `isNewUser`
- `OAuthUserInfo` record: `providerId`, `email`, `nickname`, `profileImageUrl`, `provider`
- `OAuthService`: List<OAuthClient> → Map<Provider, OAuthClient> 자동 매핑, 기존 유저 로그인/신규 가입/이메일 충돌(409)/임시 닉네임 자동생성 처리
- `OAuthController`: `POST /api/auth/oauth/{provider}` (SecurityConfig `/api/auth/**` permitAll 기존 적용으로 추가 설정 불필요)
- `application.yaml`: oauth.kakao/google/naver 클라이언트 설정 블록 추가
- `deploy.yml`: 소셜 OAuth 환경변수 6개 추가 (`KAKAO/GOOGLE/NAVER_CLIENT_ID`, `KAKAO/GOOGLE/NAVER_CLIENT_SECRET`)

### 설계 결정
- **플로우**: 프론트가 code 획득 → `POST /api/auth/oauth/{provider}` → JWT 반환 (클라이언트 사이드 방식)
  - 이유: 서버 사이드 리다이렉트는 state 검증에 세션이 필요한데, 현재 아키텍처가 JWT Stateless라 구조적 충돌
- **신규 가입 임시 닉네임**: `kakao_xxxxxxxx` (provider 소문자 + UUID 8자리), 중복 시 최대 3회 재생성
- **이메일 충돌**: 동일 이메일로 LOCAL 계정이 이미 존재하면 409 에러 반환 (소셜 로그인 불허)
- **이메일 미동의(카카오)**: email null이면 `SOCIAL_EMAIL_REQUIRED` 에러 — 카카오 개발자 콘솔에서 이메일 필수 동의 설정 필요
- **AbstractOAuthClient**: OAuthClient 인터페이스는 유지하고 추상 클래스가 HTTP 통신 공통 로직을 담는 Template Method 패턴

---

## 2026-04-21 — 페이지네이션 구현 (커서/오프셋 혼용, 20회차)

### Added
- `CursorPageResponse<T>` record — `content`, `nextCursor`, `hasNext` 필드
- `OffsetPageResponse<T>` record — `content`, `page`, `size`, `totalElements`, `totalPages`, `hasNext` 필드
- `CursorUtils` — Base64 URL-safe 인코딩/디코딩 + `parseLongId()` 헬퍼 (null → Long.MAX_VALUE sentinel)
- `Note.likeCount` 컬럼 추가 (`INT DEFAULT 0`, popular/hot 정렬용 비정규화)
- `NoteRepository` 쿼리 4종 추가: `findPublicLatestByCursor`, `findPublicPopularByCursor`, `findPublicHotIdsByCursor`(native), `findByIdInWithAlcoholAndUser`, `findMyNotesPaged`, `findMyNotesPagedByStatus`
- `NoteImageRepository.findAllByNoteIdIn()` — 이미지 일괄 조회 (N+1 방지)
- `AlcoholRepository` 커서 쿼리 2종: `findByCategoryWithCursor`, `searchByKeywordWithCursor`
- `NoteService.getPublicNotesCursor()` — latest/popular/hot 정렬 지원 커서 페이지네이션
- `NoteService.getMyNotesPaged()` — 오프셋 페이지네이션 (status 필터 포함)
- `NoteService.toResponseList()` — 이미지 일괄 조회로 목록 N+1 제거
- hot sort: 2단계 조회 (native SQL로 ID → JPQL fetch join으로 엔티티)

### Changed
- `AlcoholService.search()` → 커서 기반으로 전환, LinkedHashSet 카테고리 머지 로직 제거
- `AlcoholService.getByCategory()` → 커서 기반으로 전환
- `NoteController.getPublicNotes()` → `sort`, `cursor`, `size` 파라미터 추가
- `NoteController.getMyNotes()` → `page`, `size` 파라미터 추가, `OffsetPageResponse` 반환
- `AlcoholController.search()` / `getByCategory()` → `cursor`, `size` 파라미터 추가, `CursorPageResponse` 반환

---

## 2026-04-19 — prod DB 정비 + 출시 준비 (19회차)

### Added
- 칵테일 15개 prod DB 직접 추가 (IBA 클래식 기준: Mojito, Negroni, Old Fashioned 등) + data.sql 반영

### Changed
- prod 테스트 데이터 전체 삭제 (alcohol 4개 + 연관 노트/신고)
- data.sql prod 직접 실행 완료 (alcohol 186개 + alias 104개)
- `Alcohol.name` / `nameKo` 컬럼 unique 제약 추가 (DB 정합성 보장)
- `deploy.yml` AWS S3 환경변수 추가 (`AWS_S3_BUCKET`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)
- Note taste/aroma: NoteFlavor 중간 테이블 → String 필드로 임시 전환 (출시 일정 이유)
  - 이유: FlavorSuggestion 선택 목록 방식은 출시 후로 연기. 코드(NoteFlavor, FlavorSuggestion)는 유지
- Note.alcohol nullable 전환 + `custom_alcohol_name` 컬럼 추가 (customAlcoholName 자유 텍스트 지원)
  - alcoholId 없이도 노트 작성 가능 (AlcoholRequest 없이 바로 작성 가능, UX 개선)
  - alcohol이 있으면 customAlcoholName null 강제, alcohol이 null이면 customAlcoholName 저장
  - NoteBaseRequest: alcoholId optional, customAlcoholName(@Size max=100) 필드 추가
  - NoteService: `validateAlcoholInput()` (둘 다 없으면 INVALID_INPUT 400), `resolveAlcohol()` 헬퍼 추가

---

## 2026-04-19 — AlcoholRequest 검증 강화 및 버그 7건 수정 (18회차)

### Fixed

- **nameKo AlcoholRequest 중복 체크 누락** — `validateNoDuplicateName()`
  - 이유: nameKo는 `alcoholRepository`만 체크하고 `AlcoholRequest` 테이블은 안 봐서, 같은 nameKo로 PENDING 요청이 이미 있어도 또 요청 가능했음.
  - 수정: `existsByRequestedByAndNameKoIgnoreCase`, `existsByNameKoIgnoreCaseAndStatus` 추가.

- **nameKo AlcoholAlias 중복 체크 누락** — `validateNoDuplicateName()`
  - 이유: name은 AlcoholAlias까지 체크하는데 nameKo는 안 했음. nameKo가 이미 별칭으로 등록된 경우 감지 불가.
  - 수정: `alcoholAliasRepository.existsByAliasIgnoreCase(nameKo)` 체크 추가.

- **name↔nameKo 크로스 필드 PENDING 중복 체크 누락** — `validateNoDuplicateName()`
  - 이유: A가 `name="블레어우드 리저브"`로 PENDING 중일 때 B가 `nameKo="블레어우드 리저브"`로 요청하면 통과됨. name 체크는 `name` 컬럼만, nameKo 체크는 `nameKo` 컬럼만 봤기 때문.
  - 수정: name 체크 시 `existsByNameKoIgnoreCaseAndStatus(name, PENDING)`도 추가; nameKo 체크 시 `existsByNameIgnoreCaseAndStatus(nameKo, PENDING)`도 추가.

- **ALIAS 요청 시 공식명칭 차단 누락** — `requestAlias()`
  - 이유: `alcoholAliasRepository`만 체크해서 `Alcohol.name` / `Alcohol.nameKo`와 동일한 값을 별칭으로 요청해도 통과됨.
  - 수정: `alcoholRepository.existsByNameIgnoreCase`, `existsByNameKoIgnoreCase` 체크 추가.

- **ALIAS 요청 시 동일 alias PENDING 중복 체크 누락** — `requestAlias()`
  - 이유: 다른 유저가 이미 같은 alias를 PENDING으로 요청한 경우를 감지하지 못함.
  - 수정: `existsByAliasIgnoreCaseAndStatus(alias, PENDING)` JPQL 커스텀 쿼리 추가 (`JOIN r.aliases`).

- **ALCOHOL_ALREADY_EXISTS vs DUPLICATE_ALCOHOL_REQUEST 에러 미분리** — `validateNoDuplicateName()`, `requestAlias()`
  - 이유: DB에 이미 등록된 술과 누군가 PENDING 중인 요청이 같은 `DUPLICATE_ALCOHOL_REQUEST` 에러로 반환돼 클라이언트가 원인을 구분 불가.
  - 수정: `ErrorCode.ALCOHOL_ALREADY_EXISTS`(409) 신설. DB 존재 → `ALCOHOL_ALREADY_EXISTS`, PENDING 중 → `DUPLICATE_ALCOHOL_REQUEST`.

- **MERGED 상태 및 관련 데드코드 잔존** — `AlcoholRequest`, `AlcoholRequestStatus`
  - 이유: 18회차에 merge 엔드포인트를 제거했지만 `MERGED` 상태, `mergedToAlcohol` 필드, `merge()` 메서드가 엔티티에 남아 있었음.
  - 수정: 세 가지 모두 제거.

### Changed

- `AlcoholAliasCreateRequest`: `List<String> aliases` → `String alias` 단일 필드
  - 이유: "한 술에 한 번, 한 개의 별칭만 요청 가능" 정책 반영. 리스트 허용 시 한 요청에 여러 별칭을 넣어 정책 우회 가능.
  - `approveAlias()`도 단일 alias 처리로 단순화.

---

## 2026-04-19 — AlcoholRequest v2 리팩터링 (18회차)

### Changed
- `AlcoholRequest` 엔티티: `type`(NEW/ALIAS, NOT NULL), `targetAlcohol`(@ManyToOne nullable) 추가; `name`, `category` nullable로 변경
  - 이유: 기존 구조는 영문 name 필수라 한글 명칭만 아는 유저가 요청하기 까다로웠음. name/nameKo 중 하나만 알아도 등록 요청 가능하도록 완화.
- `AlcoholRequestCreateRequest`: `@NotBlank` 제거 → `@Size(max=100)` (서비스에서 name/nameKo 중 하나 이상 검증)
- `AlcoholRequestService.getRequests()`: `type` 파라미터 추가 (null이면 전체, NEW/ALIAS 필터 가능)
- `AlcoholRequestService.approve()`: `findPendingRequestOfType(NEW)` 적용 — ALIAS 요청에 잘못 호출되면 400 반환
- `AdminAlcoholRequestController.getRequests()`: `type` 쿼리 파라미터 추가

### Added
- `AlcoholRequestType` enum (NEW / ALIAS)
- `AlcoholAliasCreateRequest` DTO: aliases(@NotEmpty), reason(optional)
- `AlcoholRequestService.requestAlias()`: 기존 술에 별칭 추가 요청 (type=ALIAS로 저장)
- `AlcoholRequestService.approveAlias()`: ALIAS 요청 승인 → AlcoholAlias에 추가, status=APPROVED
- `POST /api/alcohol-requests/{alcoholId}/alias` — 유저: 별칭 추가 요청
- `POST /api/admin/alcohol-requests/{id}/approve-alias` — 관리자: 별칭 요청 승인
- `AlcoholRequestResponse`: type, targetAlcoholId, targetAlcoholName 필드 추가

### Removed
- `AlcoholRequestService.merge()` — 관리자 주도 별칭 병합 대신 유저 주도 ALIAS 요청으로 대체
- `AdminAlcoholRequestController.merge()` — 동일 사유
- `AlcoholRequestServiceTest.java` — 토큰 비용 대비 유지 가치가 낮아 삭제

### 결정 배경
- 기존 `merge` 엔드포인트: 관리자가 직접 대상 술을 찾아서 병합 처리해야 했음 — 운영 부담
- 새 방식: 유저가 "이 술(alcoholId)에 이 별칭을 추가해달라"고 직접 요청 → 관리자는 approve-alias만 클릭
- 유저가 실제로 쓰는 별칭을 더 잘 알고 있으므로 데이터 품질도 향상됨
- prod DB 기존 레코드 처리 필요: `UPDATE alcohol_request SET type = 'NEW' WHERE type IS NULL;`

---

## 2026-04-19 — 버그 수정 9건 (17회차)

### Fixed

- **C1. 테스트 오타** — `AlcoholRequestServiceTest.java:228` `alcoholRequestServiceet` → `alcoholRequestService`
  - 이유: 단순 오타. 컴파일 오류 발생.

- **C2. updateNote 이미지 유실 버그** — `NoteService.updateNote()`
  - 이유: 이미지 4장 전달 시 S3 먼저 삭제 → `saveImages`에서 `IMAGE_LIMIT_EXCEEDED` 예외 → DB 롤백되지만 S3 파일은 이미 삭제된 상태. 데이터 유실 발생.
  - 수정: `deleteImagesFromS3` 호출 전에 `images.size() > 3` 선체크 추가.

- **C3. Refresh-Token 헤더 누락 시 500** — `GlobalExceptionHandler`
  - 이유: `MissingRequestHeaderException` 핸들러가 없어서 generic `Exception.class`로 빠져 500 반환. 헤더 누락은 클라이언트 실수이므로 400이어야 함.
  - 수정: `MissingRequestHeaderException` 핸들러 추가 (400 반환).

- **C4. AlcoholCategory substring 과잉 매칭** — `AlcoholCategory.findByName()`
  - 이유: `contains` 방식 사용으로 `"begin"` 검색 시 GIN 카테고리 매칭, `"winery"` 검색 시 WINE 전체 반환 등 의도치 않은 카테고리 매칭 발생.
  - 수정: `equalsIgnoreCase` 방식으로 변경 — 정확히 카테고리 이름을 입력했을 때만 매칭.

- **C5. Alcohol 중복 체크 nameKo 누락** — `AlcoholRequestService.validateNoDuplicateName()`
  - 이유: 영문 name만 중복 체크하고 nameKo는 체크하지 않아, 한글명이 같은 Alcohol이 중복 생성될 수 있었음.
  - 수정: `alcoholRepository.existsByNameKoIgnoreCase(nameKo)` 체크 추가. nameKo는 null 가능이므로 null 가드(`nameKo != null && ...`) 포함.

- **H1. 빈 MultipartFile 처리 순서** — `NoteService.saveImages()`
  - 이유: 빈 파일 필터링 전에 `size > 3` 체크 → 이미지 3장 + 빈 파트 1개 전송 시 오류. 브라우저가 빈 파일 파트를 함께 전송하는 경우 발생.
  - 수정: `filter(!f.isEmpty())` 후 size 체크 순서로 변경.

- **H2. 다른 유저 PENDING 요청 중복 방지 미흡** — `AlcoholRequestService.validateNoDuplicateName()`
  - 이유: 본인 이력만 체크하고, 다른 유저가 이미 같은 이름으로 PENDING 요청을 넣은 경우를 허용. 관리자가 같은 술에 대한 요청을 중복 처리해야 하는 비효율 발생.
  - 수정: `alcoholRequestRepository.existsByNameIgnoreCaseAndStatus(name, PENDING)` 체크 추가.

- **H3. S3Service.delete() 예외 처리 없음** — `S3Service.delete()`
  - 이유: `upload()`는 try/catch 있지만 `delete()`는 없어 SDK 예외 발생 시 500 반환. 삭제 실패로 노트 삭제까지 막히는 것은 부적절 — 고아 파일 정도는 허용하는 것이 낫고 로그로 추적하면 충분.
  - 수정: try/catch 추가, 실패 시 `log.warn`만 남기고 계속 진행.

- **M1. 중복 import** — `AlcoholRequestService.java`
  - 이유: `import java.util.ArrayList` 2번 선언. 16회차 리팩터링 시 남은 잔재.
  - 수정: 중복 제거.

### Planned (다음 브랜치: feature/alcohol-request-v2)
- AlcoholRequest 리팩터링 — type(NEW/ALIAS) 분리, 영/한 자유 입력, 별칭 요청 API 신설 (자세한 내용은 context.md 3-1 항목 참고)

---

## 2026-04-17 — 노트 이미지 S3 업로드 구현 (17회차)

### Added
- `common/s3/S3Port.java` — S3 외부 시스템 연결부 인터페이스 (Port 패턴, upload/delete)
- `common/s3/S3Service.java` — S3Port 구현체 (AWS SDK v2, DefaultCredentialsProvider)
- `common/config/S3Config.java` — S3Client Bean (region 환경변수 기반)
- `NoteImageRepository.findAllByNoteId()` — 노트별 이미지 목록 조회 메서드
- `NoteResponse.imageUrls` — 이미지 URL 목록 필드 추가
- `ErrorCode` — IMAGE_UPLOAD_FAILED(500), IMAGE_LIMIT_EXCEEDED(400), INVALID_IMAGE_TYPE(400)
- `build.gradle.kts` — AWS SDK v2 s3:2.25.70 의존성 추가
- `application.yaml` — multipart 5MB/15MB 제한 + aws.s3 설정
- `application-prod.yaml` — aws.s3 설정 추가
- `application-local.yaml` — Slack 알림 설정 키 수정 (slack-webhook-url → slack.error-webhook-url)

### Changed
- `NoteController.createNote` — `@RequestBody` → `multipart/form-data` (`@RequestPart`)
- `NoteController.updateNote` — 동일하게 multipart 전환, `@Encoding`으로 Swagger note 파트 Content-Type 명시
- `NoteService.createNote/updateNote` — images 파라미터 추가, S3Port 주입, 이미지 헬퍼 추가
- `NoteService.deleteNote` — 삭제 전 S3 파일 먼저 삭제 처리
- `NoteService.toResponse()` — 오버로딩 추가 (이미지 리스트 재사용 버전으로 DB 쿼리 절약)
- `GlobalExceptionHandler` — 500 에러 `log.error` 추가 (디버깅용)
- `NoteResponse.from()` — 시그니처 변경 (`List<NoteImage>` 파라미터 추가)

---

## 2026-04-17 — 공통 패턴 정리 (16회차)

### Changed
- `AlcoholRequestService.request()`: 중복 체크 3개(if 3연속) → `validateNoDuplicateName()` private 헬퍼로 추출
  - NoteService의 `findNoteAndValidateOwner()` 패턴과 동일 — 반복되는 검증 로직을 한 곳에 모아 가독성 향상
- `AlcoholRequestService.saveAliases()`: 스트림 내 개별 `save()` 루프 → `saveAll()` 벌크 저장으로 전환
  - NoteService의 `saveAll()` 패턴과 동일 — DB 왕복 횟수 감소
- `AlcoholRequestService`: `new java.util.ArrayList<>()` FQCN 방식 → `import java.util.ArrayList` 추가 후 간결하게 사용
- `AlcoholRequestController.request()`: `ResponseEntity.ok().build()` → `ResponseEntity.noContent().build()` (200 → 204)
- `AdminAlcoholRequestController.approve/merge/reject()`: 동일하게 200 → 204로 통일
  - void 응답에 200을 쓰면 "바디가 있을 수도 있다"는 오해를 줄 수 있음. 결과물이 없는 뮤테이션은 204가 REST 관례

---

## 2026-04-16 — 술 초기 데이터 삽입 + 테스트 수정 (15회차)

### Added
- `src/main/resources/data.sql` — 술 초기 데이터 170개 + AlcoholAlias 90개
  - 위스키 64개 (블렌디드/스페이사이드/하이랜드/아일라/아메리칸/아이리시/재패니즈)
  - 와인 32개 (레드/화이트/스파클링/로제)
  - 맥주 27개 (수입 20 + 국산 7)
  - 소주 10개 / 막걸리 10개 / 사케 10개 / 보드카 6개 / 진 6개 / 럼 6개 / 테킬라 6개 / 브랜디 6개
  - AlcoholAlias: 조니워커 계열, 발렌타인, 맥캘란 등 주요 별칭 포함
  - 로컬 H2: 앱 시작 시 자동 실행 / prod MySQL: SSH로 1회 실행
  - Flyway 도입 시 `V2__seed_data.sql`로 이름만 바꾸면 그대로 활용 가능

### Changed
- `application.yaml`: `spring.jpa.defer-datasource-initialization: true` + `spring.sql.init.encoding: UTF-8` 추가
  - defer: Hibernate DDL 이후 data.sql 실행 보장
  - encoding: 한글 데이터 인코딩 오류 방지
- `application-local.yaml`: `spring.sql.init.mode: always` 추가 (로컬 H2에서 data.sql 실행)
- `AlcoholRequestServiceTest`: 테스트 3곳 수정
  - `reject()` 인자 누락 수정 — 13회차에 `rejectReason` 파라미터 추가됐는데 테스트 미반영
  - `approve_success` / `merge_success`: `saveAliases()`가 name+nameKo+alias 3개 저장하는데 `times(1)` 그대로 → `times(3)`으로 수정

### Removed
- `TastingnoteApplicationTests.java` — data.sql 한글 인코딩 문제로 contextLoads() 실패, 삭제

---

## 2026-04-16 — 출시 로드맵 확정 + 술 초기 데이터 전략 결정 (15회차)

### 결정
- **출시 전 작업 순서 확정**: 술 초기 데이터 삽입 → S3 이미지 업로드 → 소셜 로그인 → 출시
  - Tag, Like, 술 상세 페이지 API는 출시 후에 구현
- **술 초기 데이터 삽입 방법 확정**:
  - `src/main/resources/data.sql` — 로컬 H2에서 앱 시작 시 자동 실행 (인메모리라 매번 fresh)
  - prod MySQL은 SSH로 한 번만 직접 실행 (`mysql -u root -p tastingnote < data.sql`)
  - 목표: 약 150~180개 메이저 술 + AlcoholAlias 포함
  - 나중에 Flyway 도입 시 `V2__seed_data.sql`로 이름만 바꾸면 그대로 활용 가능
- **Flyway 도입 시점 확정**: 소셜 로그인 완료 후 출시 직전
  - 현재 `ddl-auto: update`는 개발 편의를 위해 유지
  - 엔티티 변경이 끝나는 시점(소셜 로그인 완료)에 도입
  - `ddl-auto: validate`로 전환 — 운영 DB 스키마 자동 변경 위험 차단
  - `build.gradle` flyway 의존성 + `V1__init_schema.sql` + `V2__seed_data.sql` 정리 한 번에 진행

---

## 2026-04-16 — 노트 수정 버그 수정 (14회차)

### Fixed
- `NoteService.updateNote()`: 수정 내용이 DB에 반영되지 않는 버그 수정
  - 원인: `noteFlavorRepository.deleteAllByNoteId()`가 `@Modifying(clearAutomatically=true)`로 선언되어 있어, 실행 후 영속성 컨텍스트(1차 캐시)가 초기화됨
  - 이로 인해 `note.update()`로 변경한 엔티티가 detached 상태가 되어 트랜잭션 커밋 시 dirty checking 대상에서 제외 → DB에 쓰이지 않음
  - PATCH 응답은 in-memory 객체 기준으로 반환되어 정상처럼 보이지만 실제 DB는 구버전 그대로
  - 해결: `NoteFlavorRepository.deleteAllByNoteId()`에 `flushAutomatically = true` 추가
    - DELETE 실행 직전에 pending 변경사항(note update)을 먼저 DB에 flush → 이후 컨텍스트 초기화되어도 이미 기록된 상태
    - 이전에 시도한 `noteRepository.save(note)` 방식은 실제로 즉시 flush를 보장하지 않아 제거

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