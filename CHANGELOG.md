# 변경/개선 이유 기록

코드를 왜 이렇게 바꿨는지를 기록하는 파일.
context.md 완료 섹션은 "무엇을 했는지"만 기록하고,
이유가 필요한 결정은 여기에 남긴다.

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

## 2026-04-11 — unpublishNote 엔드포인트 제거 결정

**결정**: `/api/notes/{noteId}/unpublish` 엔드포인트 및 관련 서비스/엔티티 메서드 제거

**이유**: 발행 후 DRAFT로 되돌리는 기능은 대부분의 서비스(인스타그램, 브런치 등)에서도 없음.
발행 후 상태 관리는 세 가지로 충분: 수정(updateNote) + 비공개 전환(isPublic=false) + 삭제.
unpublish는 PUBLISHED → DRAFT 상태 역행인데, 이는 "비공개 설정"(isPublic=false, PUBLISHED 유지)과 개념적으로 혼란스럽고 UX도 복잡해짐.
DRAFT 상태에서는 프론트엔드에서 공개/비공개 UI를 애초에 안 보여줄 예정이라 더욱 불필요.

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