# 개념 학습 노트

개발하면서 새로 접한 개념들을 정리하는 파일입니다.
Claude Code가 새로운 클래스/기능을 만들 때마다 여기에 설명을 추가합니다.

---

## HTTP 200 OK vs 204 No Content — 16회차

REST API에서 void 응답(반환값 없는 뮤테이션)에 어떤 상태 코드를 써야 하는가.

| 코드 | 의미 | 쓰는 경우 |
|------|------|----------|
| `200 OK` | 성공, 바디가 있을 수 있음 | GET 조회, 바디 있는 응답 |
| `204 No Content` | 성공, 응답 바디 없음 (설계상) | 삭제, 로그아웃, 상태 변경 등 결과물이 없는 뮤테이션 |

`ok().build()`는 "성공했는데 줄 게 없어요"처럼 어색하다.  
`noContent().build()`는 "이 API는 원래 바디를 돌려주지 않는다"는 의미를 명확히 전달한다.  
프론트엔드가 응답 바디를 파싱하려다 오류 내는 상황도 방지할 수 있다.

---

## private 헬퍼 메서드 추출 패턴 — 16회차

NoteService에서 학습한 패턴을 AlcoholRequestService에 적용.

**언제 추출하나?**
- 같은 예외를 던지는 if 블록이 연속으로 나타날 때
- 메서드 이름으로 "무엇을 하는지"를 설명할 수 있을 때

**예시**
```java
// Before: request() 안에 if 3개
if (alcoholRequestRepository.existsByRequestedByAndNameIgnoreCase(...)) throw ...;
if (alcoholRepository.existsByNameIgnoreCase(...)) throw ...;
if (alcoholAliasRepository.existsByAliasIgnoreCase(...)) throw ...;

// After: 의도가 명확한 한 줄
validateNoDuplicateName(user, req.getName());
```

**saveAll() 벌크 저장**  
스트림 내에서 `save()`를 반복 호출하면 DB 왕복이 N번 발생한다.  
리스트를 먼저 만들고 `saveAll()`로 한 번에 보내면 DB 왕복 1번으로 줄어든다.

---

## Spring Boot SQL 초기화 (`data.sql`) — 15회차

### data.sql이란?
`src/main/resources/data.sql` 파일을 두면 앱 시작 시 자동으로 SQL을 실행해준다.  
H2 같은 인메모리 DB는 매번 초기화되므로, 이 파일로 테스트용 기초 데이터를 자동 삽입할 수 있다.

### 왜 `defer-datasource-initialization: true`가 필요한가?
Spring Boot 3.x에서 JPA + data.sql 조합의 함정:
- 기본 동작: `data.sql` → Hibernate DDL (테이블 생성) 순서로 실행
- 문제: 테이블이 없는 상태에서 INSERT 시도 → 에러
- 해결: `spring.jpa.defer-datasource-initialization: true` 설정 시 순서가 반전
  - Hibernate DDL (테이블 생성) → `data.sql` 실행

### 인코딩 설정이 필요한 이유
`data.sql`에 한글이 있으면 `spring.sql.init.encoding: UTF-8` 설정 필수.  
Windows 환경의 기본 인코딩이 EUC-KR이라 한글이 깨져서 SQL 오류 발생.

### 로컬 vs 프로덕션 전략
- 로컬 H2: `spring.sql.init.mode: always` → 앱 시작마다 자동 실행 (인메모리라 매번 fresh)
- prod MySQL: Spring Boot 통해 실행하지 않고 SSH로 1회만 직접 실행
  ```bash
  mysql -u root -p tastingnote < data.sql
  ```
- 나중에 Flyway 도입 시 `V2__seed_data.sql`로 이름만 바꾸면 그대로 활용 가능

### 테스트에서 data.sql 막는 법
`src/test/resources/application.yaml`에 아래 설정을 두면 테스트에서 data.sql이 실행되지 않음:
```yaml
spring:
  sql:
    init:
      mode: never
```
테스트는 seed data가 필요 없고, 오히려 실행되면 context 로딩 속도가 느려지거나 인코딩 문제가 생길 수 있다.

---

## JWT 인증 구조

### 전체 흐름

```
[회원가입/로그인]
    → 서버가 Access Token + Refresh Token 발급
    → 클라이언트는 이후 요청마다 헤더에 Access Token 첨부
        Authorization: Bearer {accessToken}

[인증이 필요한 API 요청]
    → JwtAuthenticationFilter가 토큰 확인
        → 유효하면 → 컨트롤러 실행
        → 만료/변조 → 401 에러

[Access Token 만료 시]
    → Refresh Token으로 새 Access Token 재발급 요청
    → Refresh Token도 만료 → 재로그인
```

### Access Token vs Refresh Token

| | Access Token | Refresh Token |
|---|---|---|
| **역할** | API 요청 시 신원 증명 | Access Token 재발급용 |
| **만료** | 1시간 | 30일 |
| **저장 위치** | 클라이언트 (메모리/로컬스토리지) | 클라이언트 (쿠키/로컬스토리지) |
| **왜 짧게?** | 탈취 시 피해 최소화 | 길어야 재로그인 안 해도 됨 |

> **12회차 변경**: RT를 서버 DB에 저장하는 방식에서 **Stateless(JWT만으로 검증)** 방식으로 전환.
> `RefreshToken` 엔티티와 `RefreshTokenRepository` 삭제. RT 유효성은 JWT 서명 + 만료시간으로만 확인.
> 강제 로그아웃이 필요하면 추후 Redis 블랙리스트 방식으로 추가 예정.

---

## JPA 메모장(영속성 컨텍스트)과 clearAutomatically 부작용

### 문제 발견 (14회차)

노트를 수정하면 PATCH 응답은 수정된 값이 나오는데, 이후 조회하면 수정 전 데이터가 그대로 나오는 버그.

### JPA의 동작 방식 — 메모장 비유

JPA는 DB에서 데이터를 읽으면 **메모리에 복사본(메모장)**을 만들어 둔다.
코드에서 수정하면 일단 메모장만 바뀌고, **트랜잭션이 끝날 때 메모장 내용을 DB에 반영**한다.
(이 메모장을 공식 용어로 "영속성 컨텍스트" 또는 "1차 캐시"라고 부른다.)

### 왜 버그가 생겼나

```
1. note.update(...)           → 메모장에서 수정 (아직 DB에는 안 씀)
2. deleteAllByNoteId() 실행
   → @Modifying(clearAutomatically=true) 설정 때문에
   → 메모장 전체가 초기화됨 💥
   → 수정 중이던 note도 같이 날아감
3. 트랜잭션 종료
   → "메모장에 뭐 바뀐 게 있어?" → 아무것도 없음
   → DB에 아무것도 안 씀
4. 결과: DB는 수정 전 데이터 그대로
```

응답이 정상처럼 보인 이유: 메모장이 날아가기 전에 응답을 이미 만들었기 때문.

### clearAutomatically=true가 왜 필요한 설정인가

`@Modifying @Query`로 실행한 DELETE는 메모장을 bypass하고 DB에 직접 쿼리를 날린다.
이러면 메모장과 실제 DB가 불일치 상태가 된다 (메모장엔 아직 있는데 DB에선 삭제됨).
`clearAutomatically=true`는 이 불일치를 막기 위해 메모장을 초기화하는 옵션.
→ 필요한 설정이지만, 수정 중이던 다른 데이터까지 같이 날리는 부작용이 있음.

### 해결

DELETE 실행 직전에 메모장 내용을 먼저 DB에 기록하면 된다.
서비스 코드 대신 **Repository의 `@Modifying`에 `flushAutomatically = true`를 추가**하는 것이 실무적으로 올바른 방법.

```java
// NoteFlavorRepository
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("DELETE FROM NoteFlavor nf WHERE nf.note.id = :noteId")
void deleteAllByNoteId(@Param("noteId") Long noteId);
```

DELETE 실행 직전에 메모장의 모든 pending 변경사항(note update 포함)을 먼저 DB에 씀.
이후 `clearAutomatically = true`로 메모장이 초기화되어도 이미 기록된 상태라 문제없음.

**왜 `noteRepository.save(note)`는 충분하지 않나**
`save()`는 managed 엔티티에 대해 내부적으로 `em.merge()`를 호출하는데, 이건 즉시 DB 쓰기가 아님.
실제 SQL은 여전히 나중(Hibernate가 필요하다고 판단할 때)으로 미뤄짐.
→ 결국 clearAutomatically로 메모장이 날아가면 같은 문제 반복.

**`saveAndFlush()`는?**
즉시 flush를 강제해서 동작은 하지만, 서비스 레이어에 flush 책임이 생김.
`flushAutomatically = true`는 Repository에서 flush 책임을 지는 방식이라 더 응집력 있고 실무에서도 권장됨.

---

## 클래스별 설명

### `RefreshToken` (user/entity/RefreshToken.java)

Refresh Token을 DB에 저장하기 위한 엔티티.

```java
private User user;          // 어떤 유저의 토큰인지
private String token;       // 실제 토큰 문자열
private LocalDateTime expiresAt;  // 만료 시각

public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);  // 현재 시각이 만료 시각을 지났으면 true
}
```

**왜 DB에 저장하나?**
토큰이 탈취됐을 때 서버에서 강제로 무효화할 수 있어요.
DB에서 해당 토큰을 삭제하면 더 이상 재발급이 안 됩니다.

---

### `JwtTokenProvider` (common/jwt/JwtTokenProvider.java)

Access/Refresh Token을 생성하고 검증하는 유틸 클래스.

```java
// 토큰 생성 — userId를 토큰 안에 암호화해서 담음
public String generateAccessToken(Long userId)
public String generateRefreshToken(Long userId)

// 토큰에서 userId 꺼내기
public Long getUserId(String token)

// 토큰이 유효한지 확인 (변조/만료 체크)
public boolean validateToken(String token)
```

**`@Value("${jwt.secret}")`** — application.yaml의 값을 읽어오는 어노테이션.
secret은 환경변수(`JWT_SECRET`)로 받아서 코드에 직접 노출되지 않게 보호.

**`Keys.hmacShaKeyFor`** — 문자열 secret을 JWT 서명용 암호화 키로 변환.
이 키로 서명된 토큰은 같은 키 없이는 위조할 수 없음.

---

### `JwtAuthenticationFilter` (common/jwt/JwtAuthenticationFilter.java)

모든 HTTP 요청이 컨트롤러에 도달하기 전에 실행되는 필터.
토큰을 확인하고 유효하면 Spring Security에 인증 정보를 등록.

```java
// 요청 헤더에서 토큰 추출
// "Authorization: Bearer {token}" → "{token}" 만 잘라냄
private String resolveToken(HttpServletRequest request)

// 필터 핵심 로직
protected void doFilterInternal(...) {
    // 1. 헤더에서 토큰 추출
    // 2. 토큰 유효성 검사
    // 3. 유효하면 userId를 SecurityContext에 저장
    // 4. 다음 필터/컨트롤러로 요청 넘기기
}
```

**`OncePerRequestFilter`** — 요청당 딱 한 번만 실행되는 필터 기반 클래스.

**`SecurityContextHolder`** — Spring Security의 인증 정보 보관함.
여기에 저장해두면 컨트롤러 어디서든 현재 로그인한 유저 정보를 꺼낼 수 있음.

**`UsernamePasswordAuthenticationToken`** — "이 유저는 인증됐습니다"를 나타내는 객체.
userId를 담아서 SecurityContextHolder에 등록.

**`filterChain.doFilter`** — 이 필터 처리가 끝나고 다음 단계로 넘기는 코드.
이게 없으면 요청이 여기서 멈춰서 컨트롤러까지 못 도달함.

---

### `SecurityConfig` (common/config/SecurityConfig.java)

Spring Security 설정 클래스. JWT 필터 등록 및 URL별 인증 정책 설정.

```java
// 세션 미사용 — JWT는 서버가 상태를 저장하지 않음
.sessionCreationPolicy(SessionCreationPolicy.STATELESS)

// 인증 없이 접근 가능한 URL
.requestMatchers("/api/auth/**", "/swagger-ui/**", ...).permitAll()
// 나머지는 전부 토큰 필요
.anyRequest().authenticated()

// JwtAuthenticationFilter를 기본 로그인 필터보다 먼저 실행
.addFilterBefore(new JwtAuthenticationFilter(...), UsernamePasswordAuthenticationFilter.class)
```

**`BCryptPasswordEncoder`** — 비밀번호 단방향 암호화 도구.
DB에 암호화된 값을 저장하고, 로그인 시 입력값을 같은 방식으로 암호화해서 비교.
단방향이라 저장된 값으로 원래 비밀번호를 복원할 수 없음.

---

### `UserService` (user/service/UserService.java)

회원가입, 로그인, 토큰 재발급, 로그아웃 비즈니스 로직.

- **`signUp`** — 이메일/닉네임 중복 체크 후 비밀번호 암호화해서 저장. `Provider.LOCAL`로 일반 가입 표시.
- **`login`** — 이메일로 유저 조회 후 `passwordEncoder.matches()`로 비밀번호 비교. 에러 메시지를 통일하는 이유: "이메일이 없습니다" vs "비밀번호가 틀렸습니다"를 구분하면 해커가 이메일 존재 여부를 알 수 있기 때문.
- **`reissue`** — ~~Refresh Token 검증 후 기존 토큰 삭제하고 새 토큰 발급~~ → 12회차 변경: `JwtTokenProvider.validateAndGetUserIdFromRefreshToken()`으로 서명/만료 검증만 하고 DB 조회 없이 새 토큰 발급.
- **`logout`** — ~~DB에서 Refresh Token 삭제~~ → 12회차 변경: 서버는 아무것도 안 함. 클라이언트가 저장된 토큰을 폐기하는 것으로 로그아웃 완료.
- **`issueTokens`** — login/reissue 공통 토큰 발급 로직.
- **`@Transactional`** — DB 작업 중 오류 발생 시 전체 롤백. 데이터 일관성 보장.

---

### `UserController` (user/controller/UserController.java)

| 메서드 | URL | 설명 | 토큰 필요 |
|--------|-----|------|-----------|
| POST | `/api/auth/signup` | 회원가입 | X |
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/reissue` | 토큰 재발급 | X |
| POST | `/api/auth/logout` | 로그아웃 | O |

- **`@Valid`** — DTO의 검증 어노테이션(`@NotBlank`, `@Email` 등)을 실제로 실행. 없으면 검증이 동작 안 함.
- **`@RequestHeader("Refresh-Token")`** — 요청 헤더에서 값을 꺼냄. 클라이언트가 `Refresh-Token: {token}` 헤더로 전송.
- ~~**`SecurityContextHolder.getContext().getAuthentication().getPrincipal()`**~~ → 12회차 변경: `@CurrentUserId Long userId` 파라미터로 대체. 컨트롤러에서 SecurityContextHolder 직접 접근 제거.

---

## ddl-auto — 서버 시작 시 DB 스키마를 어떻게 처리할지 결정

`spring.jpa.hibernate.ddl-auto` 옵션. 서버가 시작될 때 JPA가 코드의 엔티티(예: User.java, Note.java)를 보고 실제 DB 테이블과 비교해서 처리하는 방식을 결정한다.

| 값 | 동작 | 언제 쓰나 |
|---|---|---|
| `create` | 기존 테이블 삭제 후 새로 생성 | 테스트 (데이터 매번 초기화) |
| `create-drop` | 시작 시 생성, 종료 시 삭제 | 단위 테스트 |
| `update` | 엔티티 변경사항을 DB에 자동 반영 | 개발 초기 (편하지만 위험) |
| `validate` | 코드와 DB가 일치하는지 확인만, 다르면 서버 시작 거부 | 운영(prod) 권장 |
| `none` | 아무것도 안 함 | Flyway 같은 별도 마이그레이션 툴 사용 시 |

**왜 prod에서 `update`가 위험한가?**
실수로 엔티티 필드명을 바꾸거나 삭제하면, 다음 배포 시 DB 컬럼이 자동으로 바뀌거나 삭제될 수 있음. 데이터 손실 위험.

**TastingNote 현재 결정:**
`update` 유지 (테스트 단계). 서비스 오픈 전 Flyway 도입 후 `validate`로 전환 예정.

**Flyway란?**
DB 스키마 변경을 SQL 파일로 버전 관리하는 도구. `V1__create_user.sql`, `V2__add_nickname.sql` 처럼 순서대로 관리. 배포 시 아직 실행 안 된 SQL 파일을 자동으로 실행해줌. `ddl-auto: update`처럼 자동이지만 **개발자가 직접 SQL을 작성**하므로 의도치 않은 변경이 없음.

---

## @Modifying — JPA에서 데이터를 변경하는 쿼리임을 선언

Spring Data JPA에서 `@Query`로 직접 쿼리를 작성할 때, SELECT가 아닌 INSERT/UPDATE/DELETE는 반드시 `@Modifying`을 함께 써야 함.

```java
// 잘못된 예 — @Modifying 없으면 에러 발생
@Query("DELETE FROM NoteFlavor nf WHERE nf.note.id = :noteId")
void deleteAllByNoteId(@Param("noteId") Long noteId);

// 올바른 예
@Modifying(clearAutomatically = true)
@Query("DELETE FROM NoteFlavor nf WHERE nf.note.id = :noteId")
void deleteAllByNoteId(@Param("noteId") Long noteId);
```

**`clearAutomatically = true`가 왜 필요한가?**
JPA는 1차 캐시(영속성 컨텍스트)에 조회한 엔티티를 저장해둠. `@Modifying`으로 DB에서 데이터를 삭제해도 이 캐시에는 삭제된 데이터가 남아있을 수 있음. 같은 트랜잭션에서 삭제 후 조회하면 이미 지운 데이터가 다시 보이는 현상(유령 데이터)이 발생할 수 있어서, `clearAutomatically = true`로 삭제 후 캐시를 자동 초기화함.

**왜 기본 derived delete가 N+1인가?**
Spring Data JPA의 `void deleteAllByNoteId(Long noteId)` 같은 메서드명 기반 삭제는 내부적으로:
1. `SELECT * FROM note_flavor WHERE note_id = ?` (전체 조회)
2. 조회된 각 행마다 `DELETE FROM note_flavor WHERE id = ?` (개별 삭제)

→ 10개면 쿼리 11번. `@Modifying @Query`는 단 1번의 DELETE 쿼리로 처리.

---

## X-Frame-Options — Clickjacking 방어 HTTP 헤더

`X-Frame-Options`는 내 사이트가 다른 사이트의 `<iframe>` 안에 삽입되는 것을 막는 보안 헤더.

**Clickjacking이란?**
공격자가 악성 사이트에 내 사이트를 보이지 않는 투명한 iframe으로 올려두고, 사용자가 "경품 당첨" 버튼을 클릭하는 줄 알고 누르면 실제로는 내 사이트의 "계정 삭제" 버튼을 누르게 되는 공격.

```
X-Frame-Options: DENY      → 어떤 도메인도 iframe 삽입 불가 (가장 강력)
X-Frame-Options: SAMEORIGIN → 같은 도메인만 iframe 허용
(없으면)                   → 누구나 iframe에 삽입 가능 (위험)
```

**TastingNote에서 제거한 이유:**
H2 콘솔(개발용 DB 관리 UI)이 내부적으로 iframe을 사용해서 `frameOptions().disable()`을 추가했는데, 이게 모든 환경(prod 포함)에 적용됐음. Spring Security 기본값(DENY)으로 복원함.

---

## findAllById / saveAll — 벌크(일괄) 조회 및 저장

`findById`와 `save`를 루프에서 반복 호출하면 N번 쿼리가 발생. `findAllById`와 `saveAll`은 한 번에 처리.

```java
// 나쁜 예 — 10개면 SELECT 10번
for (Long id : ids) {
    repository.findById(id); // 쿼리 1번씩
}

// 좋은 예 — 한 번에 IN 쿼리로 조회
repository.findAllById(ids); // SELECT * FROM ... WHERE id IN (1, 2, 3, ...)
```

```java
// 나쁜 예 — 10개면 INSERT 10번
for (Entity e : list) {
    repository.save(e);
}

// 좋은 예 — 배치 INSERT
repository.saveAll(list);
```

**주의사항:** `findAllById`는 존재하지 않는 ID를 조용히 무시함. 기존 `findById`는 없으면 에러를 던지지만, `findAllById`는 그냥 반환 목록에서 빠짐. 그래서 반환된 개수와 요청한 ID 수를 비교해서 검증해야 함.

```java
List<FlavorSuggestion> found = repository.findAllById(allIds);
if (found.size() != allIds.size()) {
    throw new BusinessException(ErrorCode.FLAVOR_NOT_FOUND); // 없는 ID가 있으면 에러
}
```

---

## 목록 정렬 — ORDER BY 없으면 순서가 보장되지 않는다

DB는 `ORDER BY` 없이 조회하면 **내부적으로 편한 순서**로 반환. 이 순서는 DB 버전, 데이터 변경, 서버 재시작 등에 따라 달라질 수 있어서 보장되지 않음.

Spring Data JPA에서 정렬을 메서드명으로 추가하는 방법:
```java
// 정렬 없음 (순서 보장 안 됨)
List<Note> findAllByUserId(Long userId);

// 생성일 내림차순 (최신순)
List<Note> findAllByUserIdOrderByCreatedAtDesc(Long userId);

// 여러 조건 복합 정렬
List<Note> findAllByUserIdOrderByCreatedAtDescUpdatedAtDesc(Long userId);
```

---

## 앞으로 추가 예정

- ~~`AlcoholService/Controller`~~ ✅ 완료
- ~~`GlobalExceptionHandler`~~ ✅ 완료
- `TagService/Controller` — 태그 자동완성, NoteTag 연결
- `LikeService/Controller` — 반응 기능

---

## 백엔드 권한 검증이 항상 필요한 이유

### "Never trust the client"
프론트에서 버튼을 안 보여줘도, 누구든 Postman이나 curl로 API를 직접 호출할 수 있음.
```
PATCH /api/notes/42   ← 다른 사람 노트 ID
Authorization: Bearer {내 토큰}
```
프론트 UI는 "보여주기"일 뿐이고, 진짜 보안은 항상 백엔드에서 해야 함.

### TastingNote에 적용한 것
- 노트 수정/삭제/발행/되돌리기: `note.getUser().getId().equals(userId)` 체크
- 비공개(isPublic=false) 또는 DRAFT 노트: 본인 외 조회 차단
- 신고: 자기 노트는 신고 불가

### 패턴
```java
Note note = noteRepository.findById(noteId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노트입니다"));
if (!note.getUser().getId().equals(userId)) {
    throw new IllegalArgumentException("본인의 노트만 수정할 수 있습니다");
}
```
리소스 조회 → 소유자 확인 → 비즈니스 로직 순서로 항상 처리.

---

## DB 인덱스와 LIKE 검색 성능

### 인덱스란?
DB가 데이터를 빠르게 찾기 위해 만들어두는 정렬된 목록. 책의 목차와 같음.
목차가 있으면 원하는 페이지를 바로 찾을 수 있지만, 목차가 없으면 처음부터 끝까지 다 읽어야 함.

### LIKE 검색과 인덱스
```sql
-- 인덱스 사용 가능 (앞부분이 고정)
WHERE name LIKE 'John%'    -- "John으로 시작하는 것" → 목차에서 John 찾아서 거기서부터 읽으면 됨

-- 인덱스 사용 불가 (앞이 불명확)
WHERE name LIKE '%John%'   -- "어딘가에 John이 포함된 것" → 전체를 처음부터 끝까지 다 읽어야 함 (풀 테이블 스캔)
```

현재 `searchByKeyword`는 `%:keyword%` 방식이라 데이터가 많아지면 느려짐.
술 DB는 수천 개 수준이라 지금은 체감 없음. 수십만 개가 되면 전환 필요.

### 나중에 전환할 MySQL Full-Text Search
DB가 단어 단위로 별도 인덱스를 만들어두는 방식. LIKE보다 훨씬 빠름.
```sql
-- LIKE 방식 (지금)
WHERE name LIKE '%블랙%'              -- 전체 스캔

-- Full-Text Search 방식 (나중에)
WHERE MATCH(name) AGAINST('블랙')    -- 단어 인덱스 조회
```
전환 시점: 데이터가 많아져서 검색 속도가 느려진다고 느껴질 때.

---

## @CurrentUserId — HandlerMethodArgumentResolver (12회차)

컨트롤러 메서드의 파라미터를 자동으로 채워주는 Spring MVC 확장 포인트.

```java
// 기존 방식 — 컨트롤러마다 반복
Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

// 개선된 방식 — 어노테이션 하나로
public NoteResponse createNote(@CurrentUserId Long userId, ...) { ... }
```

**구현 3단계:**
1. `@CurrentUserId` 어노테이션 생성 (`@Target(PARAMETER) @Retention(RUNTIME)`)
2. `CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver` 구현 — SecurityContext에서 userId 꺼내는 로직을 여기서만 관리
3. `WebMvcConfig.addArgumentResolvers()`에 등록

**비로그인 처리:** `auth == null || !(auth.getPrincipal() instanceof Long)` 이면 `null` 반환. 비로그인도 허용하는 API(예: 노트 단건 조회)에서는 컨트롤러가 null로 비로그인 여부 판단.

---

## Port / Interface 패턴 — 외부 시스템 연결부 분리 (12회차)

Slack, S3처럼 교체 가능성이 있는 외부 시스템은 인터페이스로 분리.

```
NotificationPort (interface)          ← 서비스가 의존하는 대상
└── SlackNotificationService          ← 실제 구현체 (Slack 웹훅 호출)

미래에:
└── DiscordNotificationService        ← Slack → Discord 교체 시 이것만 새로 만들면 됨
```

**장점:**
- `GlobalExceptionHandler`, `FeedbackService`는 `NotificationPort`만 알면 됨 — Slack 코드가 어디 있는지 몰라도 됨
- 나중에 알림 수단을 바꿔도 서비스 코드 수정 없음
- 테스트 시 `NotificationPort`를 가짜 구현체(Mock)로 교체 가능

**JPA Repository는 왜 같은 방식을 쓰지 않나?**
Repository는 이 프로젝트 규모에서 교체할 일이 없음. 인터페이스 추가 = 파일만 늘고 실익 없음. 외부 IO 계층(Slack, S3)에만 적용하는 것이 균형 있는 설계.

---

## OOP 헬퍼 메서드 추출 — SRP + DRY (12회차)

NoteService에서 적용한 패턴. 객체지향에서 메서드는 **한 가지 일**만 해야 한다(Single Responsibility Principle).

| 헬퍼 | 하는 일 | 적용 위치 |
|------|---------|---------|
| `buildNote(user, alcohol, request)` | Note 객체 생성만 | createNote |
| `saveFlavorsThenResponse(note, tasteIds, aromaIds)` | flavor 저장 + 응답 반환 | createNote, updateNote |
| `toResponse(note)` | DB 조회 + 응답 변환 | getNote, publishNote, stream lambda 3곳 |
| `findNoteAndValidateOwner(noteId, userId)` | 노트 조회 + 소유자 검증 | updateNote, publishNote, deleteNote |

**DRY(Don't Repeat Yourself):** 같은 코드가 여러 곳에 있으면 하나가 바뀔 때 나머지도 전부 찾아서 바꿔야 함. 헬퍼로 추출하면 한 곳만 수정하면 됨.

**`private` 접근제어가 중요한 이유 (캡슐화):** 외부에서 이 메서드를 직접 호출하면 안 되는 경우(내부 로직의 일부)에 `private`으로 숨김. 공개 API 범위를 좁게 유지할수록 나중에 내부 구현을 바꾸기 쉬워짐.

---

## 추상 클래스 DTO — 공통 필드 상속 (12회차)

`NoteCreateRequest`와 `NoteUpdateRequest`가 `isPublic` 처리 방식만 다르고 9개 필드가 동일한 문제 해결.

```java
// NoteBaseRequest (abstract)
abstract Boolean getIsPublic();  // 서브클래스마다 다른 처리 강제
// + 공통 필드 9개 (alcoholId, title, tasteIds, aromaIds, pairing, rating, description, drankAt, location)

// NoteCreateRequest
private Boolean isPublic = false;  // 기본값 false (생성 시 항상 비공개)

// NoteUpdateRequest
@NotNull
private Boolean isPublic;  // null 허용 안 함 (수정 시 명시 필요)
```

**Jackson 중복 필드 문제**: 부모 클래스에 `isPublic`을 선언하고 자식에서 오버라이드하면 JSON 직렬화 시 같은 필드가 2번 나타남. → 부모에는 선언하지 않고 `abstract getIsPublic()`만 두어 해결.

---

## @RequestParam 필수/선택 옵션

```java
// 필수 (기본값) — 파라미터 없으면 400 에러
@RequestParam AlcoholCategory category

// 선택 — 파라미터 없으면 null
@RequestParam(required = false) AlcoholCategory category
```

현재 `GET /api/alcohols`는 category가 필수라서 카테고리 없이 전체 조회 불가.
나중에 "전체 술 목록" 기능이 필요하면 `required = false`로 바꾸고,
category가 null이면 전체 반환, 있으면 필터링하는 방식으로 확장하면 됨.

---

## GlobalExceptionHandler (common/exception/GlobalExceptionHandler.java)

### 왜 만들었나?
예외 처리 없이는 에러 발생 시 500 + HTML 에러 페이지가 내려가서 프론트가 파싱을 못 함.
모든 API에 공통 에러 응답 형식(`ApiResponse.fail(message)`)을 적용하기 위해 만듦.

### 핵심 어노테이션
**`@RestControllerAdvice`** — 애플리케이션 전체에서 발생하는 예외를 한 곳에서 처리하는 클래스임을 선언.
**`@ExceptionHandler`** — 특정 예외 타입이 발생했을 때 실행할 메서드를 지정.

### 우선순위 규칙
Spring은 **가장 구체적인 타입부터** 매칭시킴.
```
IllegalArgumentException 발생 → IllegalArgumentException 핸들러
MethodArgumentNotValidException 발생 → MethodArgumentNotValidException 핸들러
그 외 모든 예외 → Exception 핸들러 (가장 넓은 범위, 마지막에 처리)
```

### 현재 처리 목록
| 예외 | 원인 | HTTP 상태 |
|------|------|-----------|
| `IllegalArgumentException` | 비즈니스 로직 에러 (존재하지 않는 리소스, 중복 등) | 400 |
| `MethodArgumentNotValidException` | `@Valid` 검증 실패 (필수값 누락, 형식 오류 등) | 400 |
| `Exception` | 예상 못한 서버 에러 | 500 |

### 나중에 추가하는 방법
커스텀 예외 클래스(`NotFoundException` 등)를 만들고 핸들러 하나만 추가하면 바로 적용됨.

---

## Git 브랜치 머지 방향

머지는 **덮어쓰기가 아니라 합치기**다. 방향이 두 가지로 나뉜다.

### feature → main (내 작업을 main에 올리기)
기능 개발 완료 후 PR을 열어 main에 머지.

### main → feature (main 변경사항을 내 브랜치로 가져오기)
```bash
git merge main
```
예: flavor 버그 수정이 main에 머지된 후, alcohol-api 브랜치에서 위 명령 실행.
→ main의 변경사항만 추가로 들어오고, 내가 만든 파일(AlcoholController 등)은 그대로 유지됨.

**Git이 하는 일**: "main에는 있는데 내 브랜치에 없는 변경사항"만 가져와서 추가. 내 파일을 덮어쓰지 않는다.

### 실무 흐름 예시
```
1. flavor 버그 수정 → main 머지
2. alcohol-api에서 git merge main 실행 (flavor 수정사항 반영)
3. 로컬에서 테스트 확인
4. alcohol-api → main 머지
```

### 충돌(Conflict)이 나는 경우
같은 파일을 양쪽(main과 내 브랜치)에서 동시에 수정했을 때만 발생.
flavor와 alcohol은 건드리는 파일이 다르므로 충돌 가능성 거의 없음.

---

## 서비스 설계 개념

### 크라우드소싱 (Crowdsourcing)

유저들이 직접 데이터를 등록하고, 관리자가 검토 후 승인하는 방식으로 DB를 키우는 전략.

**Vivino 사례**
- 와인 앱 Vivino는 초반에 바코드 스캔 + 유저 등록 방식으로 빠르게 와인 DB를 구축했음
- 유저가 새 와인을 등록하면 커뮤니티가 정보를 보완(생산지, 품종, 빈티지 등)하는 방식
- 현재 전 세계 2억 개 이상의 와인 리뷰 보유 — 이게 가능했던 이유가 크라우드소싱
- 관리자가 직접 다 입력했다면 절대 불가능한 규모

**TastingNote에 적용한다면**
```
유저: "발베니 12년 더블우드" 등록 요청 + 별칭: ["발베니 12", "Balvenie 12"]
    → DB에 PENDING 상태로 저장
    → 관리자 검토 (DBeaver 등 DB 툴로 직접 승인 가능 — 초반엔 어드민 페이지 불필요)
        → 승인 → Alcohol 테이블에 정식 등록 + AlcoholAlias 테이블에 별칭 등록
        → 거절 → 요청 REJECTED 처리
```

**별칭도 유저가 제안하는 이유**
관리자가 별칭을 직접 입력하는 방식은 작업량이 너무 많음.
유저가 실제로 검색할 때 쓰는 별칭을 관리자보다 유저가 더 잘 알고 있음.
예: "블랙라벨", "JW Black" 같은 별칭은 술을 마시는 사람들 사이에서 자연스럽게 쓰이는 표현.
별칭이 많을수록 검색 품질이 올라가고 유저가 원하는 술을 더 쉽게 찾을 수 있음.

**크라우드소싱 없을 때의 문제**
같은 술이 여러 이름으로 저장될 수 있음:
- "발베니 12년"
- "The Balvenie 12"
- "발베니 더블우드 12년"

→ 나중에 "이 술을 마신 사람들" 기능을 만들 수 없게 됨

**구현 시 필요한 것**
- `AlcoholRequest` 테이블: name, nameKo, aliases(별칭 목록), status(PENDING/APPROVED/REJECTED), requesterId
- 요청 API + 승인 API 2개
- 어드민 페이지 없이 초반엔 DB 툴로 직접 관리 가능
- 요청이 많아지면 그때 어드민 페이지 추가

---

### 어드민 페이지란?

서비스 운영자(관리자)만 접근 가능한 관리용 페이지.

**주요 기능**
- 술 DB 관리 (등록/수정/삭제)
- 유저 등록 요청 승인/거절
- 신고된 노트 처리
- 유저 계정 정지/삭제
- 통계 (가입자 수, 노트 수, 인기 술 등)

**지금 만들지 않는 이유**
1. 초반엔 유저도 없고 관리할 데이터도 없음 → DB 툴로 직접 관리가 더 빠름
2. 만들려면 JWT에 `ROLE_ADMIN` 권한을 추가하고 URL별 권한 체크 로직이 필요 → 지금은 불필요한 복잡도
3. 유저가 늘어나서 직접 DB 관리가 힘들어질 때 만들어도 충분히 늦지 않음

---

## 신고(Report) 기능

### 왜 만들었나?
공개 피드가 생기면 부적절한 콘텐츠가 올라올 수 있음.
삭제/차단 같은 자동 처리는 복잡하므로, 우선 **신고 내용을 DB에 기록만 하고 관리자가 직접 처리하는 방식**으로 시작.
나중에 유저가 늘어나면 자동 처리 로직 추가 가능.

### 구조

```
ReportReason (Enum)   — 신고 사유 종류
ReportStatus (Enum)   — 처리 상태
Report (Entity)       — 신고 기록
ReportRepository      — DB 조회/저장
ReportRequest (DTO)   — 신고 요청 데이터
ReportService         — 비즈니스 로직
ReportController      — API 엔드포인트
```

### 각 파일 설명

**ReportReason.java**
신고 사유를 Enum으로 관리. String으로 받으면 "욕설", "욕", "나쁜말"처럼 제각각으로 들어오기 때문에 Enum으로 강제.
```java
SPAM          // 스팸 / 홍보
INAPPROPRIATE // 부적절한 내용
FALSE_INFO    // 허위 정보
OTHER         // 기타 (reasonDetail에 직접 입력)
```

**ReportStatus.java**
신고 처리 상태. 초반엔 PENDING만 쌓이고 관리자가 확인 후 RESOLVED로 바꾸는 방식.

**Report.java**
신고 기록을 저장하는 엔티티.
- `reporter` — 신고한 유저 (ManyToOne)
- `note` — 신고된 노트 (ManyToOne)
- `reason` — 신고 사유 (Enum)
- `reasonDetail` — 기타(OTHER)일 때만 입력받는 텍스트
- `status` — 처리 상태, 기본값 PENDING

**ReportRepository.java**
`existsByReporterIdAndNoteId` 메서드 하나가 핵심.
같은 유저가 같은 노트를 중복 신고하는 것을 막기 위해 사용.

**ReportService.java**
신고 처리 로직. 두 가지를 확인:
1. 중복 신고 여부 체크 → 이미 신고했으면 에러
2. 신고자/노트 존재 여부 확인 후 DB에 저장

**ReportController.java**
`POST /api/notes/{noteId}/report` — JWT에서 신고자 userId를 추출하고 서비스에 전달.

### API 사용 예시
```json
POST /api/notes/42/report
Authorization: Bearer {accessToken}

{
  "reason": "OTHER",
  "reasonDetail": "허위 리뷰 같아요"
}
```

---

## Swagger 문서화 어노테이션

### 왜 만들었나?
프론트 개발자(친구)가 Swagger UI에서 각 API가 어떤 역할인지 설명이 없어서 매번 코드를 직접 확인해야 했음.
어노테이션 3개를 추가해서 Swagger UI만 봐도 API를 이해하고 테스트할 수 있게 만듦.

### 각 어노테이션 설명

**`@Tag`** — 클래스에 붙여서 API를 그룹으로 묶음. Swagger UI에서 탭처럼 분리되어 보임.
```java
@Tag(name = "인증", description = "회원가입, 로그인, 토큰 관련 API")
public class UserController { ... }
```
→ Swagger UI에서 "인증" 탭 아래 UserController의 API들이 묶여서 보임

**`@Operation`** — 메서드에 붙여서 API 이름과 설명 표시. `summary`는 짧은 제목, `description`은 상세 설명.
```java
@Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
@PostMapping("/login")
public ResponseEntity<...> login(...) { ... }
```

**`@SecurityRequirement`** — JWT 토큰이 필요한 API에 자물쇠 아이콘 표시.
Swagger UI 우측 상단 **Authorize** 버튼으로 토큰을 한 번 입력하면, 이 어노테이션이 붙은 모든 API에 자동으로 토큰 적용.
```java
@SecurityRequirement(name = "bearerAuth")
@PostMapping("/logout")
public ResponseEntity<...> logout() { ... }
```

**`SecurityScheme` 등록** — `@SecurityRequirement`가 동작하려면 SwaggerConfig에 `bearerAuth`라는 이름으로 JWT 방식을 먼저 등록해야 함.
```java
.components(new Components()
    .addSecuritySchemes("bearerAuth", new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")));
```

### JWT 필요 여부 기준
| API | 토큰 필요? | 이유 |
|-----|-----------|------|
| 회원가입, 로그인, 토큰 재발급 | X | 로그인 전 접근 가능해야 함 |
| 공개 피드 조회, 노트 단건 조회 | X | 비로그인 유저도 볼 수 있음 |
| 로그아웃, 노트 생성/수정/삭제, 신고 | O | 본인 확인이 필요한 작업 |

---

## `@Column(columnDefinition)`로 소수점 타입 지정하기

### 왜 만들었나?
`rating` 필드를 `Double` 타입 + `@Column(precision = 2, scale = 1)`로 설정했더니 앱 실행 시 아래 에러 발생.
```
scale has no meaning for SQL floating point types
```
Hibernate 6부터 `Double` 같은 부동소수점 타입에는 `scale` 지정 불가.
`precision`/`scale`은 `BigDecimal` 같은 고정 소수 타입에서만 사용 가능.

### 해결
`columnDefinition`으로 DB 컬럼 타입을 직접 지정하면 Hibernate의 타입 검사를 우회 가능.
```java
// 변경 전 — Hibernate 6에서 오류
@Column(precision = 2, scale = 1)
private Double rating;

// 변경 후 — 정상 동작
@Column(columnDefinition = "DECIMAL(2,1)")
private Double rating;
```

**`DECIMAL(2,1)`** — 전체 자릿수 2자리, 소수점 1자리. 즉 1.0 ~ 9.9 범위 저장 가능.
H2, MySQL 둘 다 `DECIMAL` 타입을 지원하기 때문에 환경에 따라 코드 변경 불필요.

---

## N+1 쿼리 문제와 해결

### N+1이란?
목록을 조회할 때 연관 데이터를 가져오기 위해 쿼리가 N+1번 실행되는 현상.
"1"은 목록 조회 쿼리, "N"은 각 항목마다 연관 데이터를 가져오는 쿼리.

```
노트 50개 목록 조회
    → SELECT * FROM note (1번)
    → SELECT * FROM users WHERE id = 1 (note[0]의 user)
    → SELECT * FROM users WHERE id = 2 (note[1]의 user)
    → ... (50번)
    → SELECT * FROM alcohol WHERE id = 5 (note[0]의 alcohol)
    → ... (50번)
총 101번 쿼리
```

### 왜 LAZY 로딩을 쓰는데 이런 문제가?
LAZY 로딩은 연관 데이터를 "실제로 접근할 때만" DB에서 가져옴.
단건 조회처럼 연관 데이터가 필요 없는 경우엔 쿼리를 아예 안 보냄 → 효율적.

문제는 목록 조회에서 루프를 돌 때:
```java
// 이 코드가 내부에서 실행되면
notes.stream().map(NoteResponse::from)  // from() 안에서 note.getUser() 호출
// → 각 note마다 SELECT * FROM users WHERE id = ? 실행
```
LAZY의 장점이 오히려 목록에서는 독이 됨.

### 왜 EAGER로 바꾸면 안 되나?
EAGER는 항상 JOIN을 강제함.
```java
// note 하나를 조회해도 항상 user와 alcohol을 JOIN해서 가져옴
noteRepository.findById(noteId)
// → SELECT * FROM note LEFT JOIN users LEFT JOIN alcohol ...
```
"note가 몇 개 있는지만 확인"하거나 "noteId만 필요한" 경우에도 불필요한 JOIN 발생.
특히 연관 관계가 많은 엔티티에서 EAGER를 쓰면 쿼리가 거대해짐.

### 해결: @EntityGraph
LAZY는 유지하되, 목록 조회 메서드에만 "이 쿼리에서는 JOIN으로 한 번에 가져와"라고 지정.

```java
@EntityGraph(attributePaths = {"user", "alcohol"})
List<Note> findAllByIsPublicTrueAndStatus(NoteStatus status);
```

결과:
```sql
-- @EntityGraph 적용 후
SELECT n.*, u.*, a.*
FROM note n
LEFT JOIN users u ON n.user_id = u.id
LEFT JOIN alcohol a ON n.alcohol_id = a.id
WHERE n.is_public = true AND n.status = 'PUBLISHED'
-- 101번 → 1번
```

### @EntityGraph vs JOIN FETCH 비교
| | @EntityGraph | JOIN FETCH |
|---|---|---|
| 작성 방식 | 어노테이션 | JPQL 직접 작성 |
| 코드 가독성 | 높음 | 낮음 (쿼리 직접 씀) |
| 유연성 | 낮음 | 높음 (복잡한 조건 가능) |
| 언제 쓰나 | Spring Data JPA 메서드와 함께 | 복잡한 조인 조건이 있을 때 |

이 프로젝트처럼 메서드 이름 쿼리를 쓰는 경우엔 @EntityGraph가 더 깔끔함.

---

## FetchType.LAZY vs EAGER 정리

| | LAZY | EAGER |
|---|---|---|
| 연관 데이터 로딩 시점 | 실제 접근할 때 | 항상 (조회 즉시) |
| 단건 조회 | 효율적 | 불필요한 JOIN 발생 가능 |
| 목록 조회 | N+1 주의 | JOIN 1번으로 해결되지만 항상 비용 발생 |
| JPA 권장 | @ManyToOne 기본값 EAGER → 명시적으로 LAZY 설정 권장 | |

**결론**: @ManyToOne은 항상 LAZY로 설정하고, 목록 조회가 필요한 곳에서만 @EntityGraph로 해결하는 게 가장 정교한 방식.

---

## 낙관적 락(Optimistic Lock) vs 비관적 락(Pessimistic Lock)

### 왜 필요한가?
같은 데이터를 여러 곳에서 동시에 수정하려 할 때 충돌이 발생함.
예: 노트를 폰과 노트북에서 동시에 열고 둘 다 수정 후 저장 → 먼저 저장한 내용이 사라짐.

### 비관적 락(Pessimistic Lock)
"충돌이 자주 일어날 것"이라고 비관적으로 가정하고, 처음부터 잠금.
```
A가 note 42를 읽음 → DB에서 해당 레코드에 잠금(SELECT FOR UPDATE)
B가 note 42를 읽으려 함 → A가 저장할 때까지 대기
A 저장 완료 → 잠금 해제 → B가 읽을 수 있음
```
- 장점: 충돌이 절대 발생하지 않음
- 단점: 대기 시간 발생, 잠금이 겹치면 데드락 위험, 트래픽 많을수록 병목

### 낙관적 락(Optimistic Lock)
"충돌이 드물 것"이라고 낙관적으로 가정하고, 잠금 없이 진행 후 저장 시 확인.
```
A가 note 42를 읽음 (version = 3)
B가 note 42를 읽음 (version = 3)
A가 먼저 저장 → version이 3인지 확인 → 맞음 → 저장 성공 → version = 4
B가 나중에 저장 → version이 3인지 확인 → 실제는 4 → 불일치 → OptimisticLockException 발생
→ 클라이언트에 "다른 곳에서 이미 수정됨, 새로고침 필요" 안내
```
- 장점: 평상시엔 잠금 없이 작동, 성능 부하 없음
- 단점: 충돌 발생 시 재시도 로직이 클라이언트에 필요

### 왜 TastingNote에서는 낙관적 락?
노트 편집은 같은 노트를 두 곳에서 동시에 수정하는 경우가 드묾.
충돌이 드문 상황에서 비관적 락으로 항상 DB 잠금을 거는 건 오버헤드.
낙관적 락은 평상시 성능을 희생하지 않으면서 실제 충돌만 처리.

### Spring JPA 구현
```java
// Note 엔티티에 추가
@Version
private Long version;
// Hibernate가 자동으로 UPDATE 시 WHERE version = ? 조건 추가
// 버전이 달라졌으면 OptimisticLockException 발생
```

---

## 캐싱(Caching) 개념

### 캐싱이란?
자주 요청되는 데이터를 메모리(RAM)에 저장해두고, 같은 요청 시 DB까지 가지 않고 메모리에서 바로 응답하는 방식.

```
[캐싱 없을 때]
유저 요청 → 서버 → DB 조회(수십 ms) → 응답

[캐싱 있을 때 - 첫 번째 요청]
유저 요청 → 서버 → DB 조회(수십 ms) → 메모리에 저장 → 응답

[캐싱 있을 때 - 이후 같은 요청]
유저 요청 → 서버 → 메모리에서 즉시 응답(수 μs)
```

DB: 수십 ms / 메모리: 수 μs → 수천 배 차이.

### 어떤 데이터에 적합한가?
캐싱은 **읽기가 많고 변경이 적은 데이터**에 효과적.

| 데이터 | 캐싱 적합? | 이유 |
|--------|-----------|------|
| 술(Alcohol) 정보 | ✅ | 관리자만 수정, 변경 빈도 낮음 |
| FlavorSuggestion 목록 | ✅ | 거의 바뀌지 않음 |
| 공개 피드 | ❌ | 실시간으로 계속 바뀜 |
| 내 노트 목록 | ❌ | 수정/삭제 빈번 |

### 캐시 무효화(Cache Invalidation)
캐싱의 핵심 어려움 — "언제 캐시를 지울 것인가".
캐시가 살아있는 동안 원본 데이터가 바뀌면 유저에게 오래된 정보를 보여주게 됨.

해결: 데이터가 바뀌는 시점에 관련 캐시를 명시적으로 삭제.
```java
@CacheEvict(value = "alcoholSearch", allEntries = true)
public void addAlcohol(...) { ... }  // 술 추가 시 검색 캐시 전체 삭제
```

### 로컬 캐시 vs Redis
**로컬 캐시(Spring Cache 기본)**: 서버 메모리에 저장.
- 서버가 1대일 때 충분.
- 서버가 여러 대이면 각 서버의 캐시가 달라지는 문제 발생.

**Redis**: 별도 캐시 서버에 저장, 모든 서버가 공유.
- 서버가 여러 대로 늘어날 때 필요.
- 서버가 재시작돼도 캐시 유지 가능.

TastingNote는 지금 서버 1대 → 로컬 캐시로 시작, 스케일아웃 시 Redis로 전환 예정.

---

## Full-Text Search vs LIKE 검색

### LIKE 검색의 두 가지 문제

**1. 성능 문제**
```sql
WHERE name LIKE '%블랙%'
```
앞에 `%`가 붙으면 인덱스를 쓸 수 없어서 테이블 전체를 처음부터 끝까지 읽음.
술 DB가 수천 개 이상 되면 느려짐.

**2. 품질 문제**
모든 결과를 동등하게 취급 — "블랙라벨" 검색 시 정확히 일치하는 것과 부분 포함하는 것이 순서 없이 섞여 나옴.

### Full-Text Search의 동작 방식
DB가 미리 각 단어를 쪼개서 **역색인(Inverted Index)** 을 만들어 둠.

```
[역색인 예시]
"조니워커" → [alcohol_id: 1]
"블랙라벨" → [alcohol_id: 1]
"블랙"     → [alcohol_id: 1, 3, 7]
"라벨"     → [alcohol_id: 1, 2]
```

"블랙" 검색 시 → 역색인에서 "블랙" 바로 찾아서 [1, 3, 7] 반환.
책의 색인에서 단어 찾는 것처럼 빠름 — 전체 스캔 불필요.

### Relevance Score (관련도 점수)
Full-Text Search는 각 결과에 점수를 매겨서 정렬 가능.
- 제목에서 검색어와 정확히 일치 → 높은 점수
- 부분 일치 또는 별칭에서 발견 → 낮은 점수

Vivino가 이 방식으로 와인 검색 결과를 정렬함.

### MySQL Full-Text Search 적용 방법
```sql
-- 컬럼에 FULLTEXT 인덱스 추가
ALTER TABLE alcohol ADD FULLTEXT INDEX ft_name (name, name_ko);

-- 검색 시
SELECT *, MATCH(name, name_ko) AGAINST('블랙' IN BOOLEAN MODE) AS score
FROM alcohol
WHERE MATCH(name, name_ko) AGAINST('블랙' IN BOOLEAN MODE)
ORDER BY score DESC;
```
MySQL 8.0 이상에서 별도 설치 없이 바로 사용 가능.

### 단계적 전환 계획
1단계: LIKE (현재) — 술 DB가 작을 때
2단계: MySQL Full-Text Search — 수천 개 이상, 검색 품질 개선 필요할 때
3단계: Elasticsearch — 수십만 개 이상, 복잡한 검색 로직 필요할 때 (현재 계획 없음)

---

## 다대다(M:N) 관계와 중간 테이블

### 언제 필요한가
하나의 Note에 여러 FlavorSuggestion이 붙을 수 있고, 하나의 FlavorSuggestion이 여러 Note에 붙을 수 있을 때 — 이게 다대다 관계.

### JPA @ManyToMany를 쓰지 않는 이유
JPA가 자동으로 중간 테이블을 만들어주지만:
- 중간 테이블에 컬럼을 추가할 수 없음 (예: type, createdAt)
- 중간 테이블을 직접 조회/삭제하기 어려움
- 실무에서는 거의 사용 안 함

### 직접 중간 엔티티를 만드는 방식 (이 프로젝트 선택)
```
Note (1) ──── (N) NoteFlavor (N) ──── (1) FlavorSuggestion
                  ┌──────────────┐
                  │ note_id      │
                  │ flavor_id    │
                  │ type (TASTE/AROMA) │
                  └──────────────┘
```
- NoteFlavor가 독립적인 엔티티라서 직접 조회/삭제 가능
- type 컬럼으로 맛(TASTE)과 향(AROMA)을 한 테이블에서 구분
- uniqueConstraint(note_id, flavor_id, type)으로 같은 노트에 같은 맛/향 중복 방지

### 왜 Vivino 방식(선택만)을 선택했나
자유 텍스트로 저장하면 "바닐라", "바닐라향", "vanilla"가 모두 다른 데이터로 쌓임.
선택 전용으로 하면 데이터가 일관되어서:
- "이 술에서 바닐라 향을 느낀 사람이 몇 명인지" 집계 가능
- Discovery 기능("바닐라 향이 난다고 기록한 노트들")이 정확하게 동작
- 술 상세 페이지의 맛/향 분포 차트 구현 가능

---

## Pull Request (PR)

### PR이란?
"이 브랜치의 변경사항을 main에 넣어주세요"라는 **요청서**.
feature 브랜치에서 개발 완료 후, main에 바로 머지하지 않고 PR을 통해 기록을 남기는 방식.

### 왜 쓰는가?
- **기록**: GitHub에 PR 페이지가 남아서 "어떤 변경이 왜 들어갔는지" 추적 가능
- **코드 리뷰**: 팀원이 코드를 보고 댓글/승인 가능 (혼자 개발 시에도 기록용으로 유효)
- **포트폴리오**: 면접관이 GitHub 볼 때 기능 단위 개발 이력이 명확히 보임

### PR 만드는 방법 (GitHub 웹)
1. GitHub 레포지토리 페이지 접속
2. "Compare & pull request" 노란 배너 클릭 (또는 Pull requests 탭 → New pull request)
3. `base: main` ← `compare: feature/브랜치명` 설정
4. 제목 + 본문 작성 → Create pull request
5. 하단 "Merge pull request" → "Confirm merge" 클릭

### 머지 방식 3가지
| 방식 | 설명 | 언제 쓰는가 |
|---|---|---|
| **Create a merge commit** | 머지 커밋 하나 추가, 기존 커밋 유지 | 기본값, 커밋이 의미 있는 단위일 때 |
| **Squash and merge** | 여러 커밋을 1개로 합쳐서 머지 | 잡다한 커밋이 많을 때 깔끔하게 |
| **Rebase and merge** | 커밋을 그대로 main에 얹음, 머지 커밋 없음 | 직선 히스토리를 원할 때 |

### 파생 브랜치와 머지 순서
feature/A에서 feature/B를 파생한 경우 (B가 A의 커밋을 포함):
- B만 머지해도 A의 커밋이 자동으로 포함됨 (Git은 커밋 단위로 관리)
- A를 먼저 머지해도 B 머지 시 중복 커밋이 들어가지 않음 (Git이 알아서 건너뜀)

### 로컬 main vs origin/main 차이
```
git merge main        ← 내 컴퓨터에 있는 main 기준 (오래됐을 수 있음)
git merge origin/main ← GitHub에 있는 main 기준 (최신)
```
PR 머지 후 다른 브랜치에서 main 내용을 가져올 때는 `git merge origin/main`이 안전.
`git fetch origin` 먼저 실행하면 origin/main 정보가 갱신됨.

### 머지 충돌(Conflict)이란?
두 브랜치에서 **같은 파일의 같은 부분**을 서로 다르게 수정했을 때 Git이 어느 쪽을 쓸지 몰라서 사람에게 선택을 맡기는 것.

```
<<<<<<< HEAD          ← 내 브랜치(현재)의 코드
현재 브랜치 코드
=======
머지하려는 브랜치의 코드
>>>>>>> origin/main   ← 가져오려는 브랜치의 코드
```

해결 방법:
1. 파일을 열어서 `<<<<<<<`, `=======`, `>>>>>>>` 마커를 모두 제거
2. 최종적으로 남길 코드만 남김 (한쪽 선택 or 두 코드 합치기)
3. `git add` → `git commit`

---

## 공통 에러 처리 구조 (2026-04-07)

### 왜 만드나?

기존에는 예외를 던질 때 이렇게만 했어요:

```java
throw new IllegalArgumentException("존재하지 않는 노트입니다");
```

이 방식의 문제:
- `IllegalArgumentException`은 무조건 **400**으로 반환됨
- "노트를 못 찾았다" → 404, "권한 없다" → 403이 맞지만 표현할 방법이 없음
- 에러 메시지가 코드 여기저기에 흩어져 있어서 관리가 어려움

---

### 구조

```
ErrorCode (enum)          ← 에러 종류 목록 + 상태코드 + 메시지 한 곳에 관리
BusinessException         ← ErrorCode를 담아서 던지는 커스텀 예외
ErrorResponse (record)    ← 에러 발생 시 프론트에 보내는 응답 DTO
GlobalExceptionHandler    ← BusinessException을 잡아서 응답으로 변환
```

---

### `ErrorCode.java` — enum

```java
USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 유저입니다")
EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다")
FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "FORBIDDEN_ACCESS", "접근 권한이 없습니다")
```

각 에러마다 3가지 정보를 갖고 있음:
- `HttpStatus` → HTTP 상태코드 (404, 409, 403 등)
- `code` → 에러 식별 문자열 (프론트에서 분기 처리용)
- `message` → 사람이 읽는 에러 메시지

**HTTP 상태코드 기준:**
| 코드 | 의미 | 언제 쓰나 |
|---|---|---|
| 400 | Bad Request | 입력값이 잘못됐을 때 |
| 401 | Unauthorized | 로그인 안 됐거나 토큰이 잘못됐을 때 |
| 403 | Forbidden | 로그인은 됐지만 권한이 없을 때 (남의 노트 수정 등) |
| 404 | Not Found | 요청한 리소스가 없을 때 |
| 409 | Conflict | 중복 데이터 (이미 있는 이메일, 이미 신고한 노트 등) |
| 500 | Internal Server Error | 서버에서 예상 못한 오류 발생 |

---

### `BusinessException.java` — 커스텀 예외

```java
// 사용 전
throw new IllegalArgumentException("존재하지 않는 노트입니다"); // 무조건 400

// 사용 후
throw new BusinessException(ErrorCode.NOTE_NOT_FOUND); // 자동으로 404
```

`RuntimeException`을 상속하기 때문에 `throws` 선언 없이 어디서든 던질 수 있음.

---

### `ErrorResponse.java` — record DTO

```java
public record ErrorResponse(boolean success, String errorCode, String message) {}
```

**record란?**
Java 16에서 도입된 문법. 데이터를 담기만 하는 클래스를 한 줄로 선언할 수 있음.
생성자, getter, equals, hashCode, toString 자동 생성.
불변(immutable) 객체 — 한번 만들면 값 변경 불가.

에러 응답 형태:
```json
{
  "success": false,
  "errorCode": "NOTE_NOT_FOUND",
  "message": "존재하지 않는 노트입니다"
}
```

`errorCode`가 있어서 프론트에서 `if (errorCode === 'INVALID_TOKEN') { 로그인페이지로이동() }` 같은 처리 가능.

---

### `GlobalExceptionHandler.java` — 예외 한 곳에서 처리

`@RestControllerAdvice` — 모든 컨트롤러에서 발생하는 예외를 한 곳에서 잡는 어노테이션.

```java
// BusinessException 발생 시 → ErrorCode의 상태코드로 응답
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();
    return ResponseEntity.status(errorCode.getStatus())
            .body(ErrorResponse.of(errorCode));
}

// @Valid 검증 실패 시 → 400 + 어떤 필드가 잘못됐는지 메시지
@ExceptionHandler(MethodArgumentNotValidException.class)
...

// 그 외 예상 못한 모든 예외 → 500
@ExceptionHandler(Exception.class)
...
```

**왜 한 곳에서 처리하나?**
예외 처리 코드가 각 컨트롤러마다 흩어지면 유지보수가 어려움.
`@RestControllerAdvice` 하나로 전체 예외를 일관되게 처리할 수 있음.

---

### 전체 흐름 요약

```
서비스 코드
  throw new BusinessException(ErrorCode.NOTE_NOT_FOUND)
        ↓
GlobalExceptionHandler가 잡음
        ↓
ErrorCode에서 HttpStatus.NOT_FOUND(404) 꺼냄
        ↓
프론트에 응답:
{
  "success": false,
  "errorCode": "NOTE_NOT_FOUND",
  "message": "존재하지 않는 노트입니다"
}
```

---

## NullPointerException (NPE) 방어 — DTO @NotNull

### NPE란?

Java에서 `null`인 변수를 사용하려 할 때 발생하는 오류.

```java
List<Long> tasteIds = null;

for (Long id : tasteIds) {  // 💥 NPE 발생! null을 순회할 수 없음
    ...
}
```

### 왜 발생했나?

`NoteCreateRequest`, `NoteUpdateRequest`의 `tasteIds`, `aromaIds` 필드는 기본값으로 `new ArrayList<>()`를 가짐.
하지만 클라이언트가 JSON에 `"tasteIds": null`을 **명시적으로** 보내면 Jackson이 기본값을 null로 덮어씀.
그 결과 `NoteService.saveFlavors()` 내부 for-each에서 NPE 발생 → 서버 500 에러.

```json
// 이런 요청이 오면 서버가 NPE로 터짐
{
  "title": "조니워커",
  "tasteIds": null,
  "rating": 4.0
}
```

### 원칙: 서버는 클라이언트를 믿으면 안 된다

일반 사용자는 프론트 UI를 통해 정상적인 값을 보내지만,
Swagger나 Postman으로 직접 API에 이상한 값을 보내는 경우를 항상 대비해야 함.
서버가 NPE로 500 에러를 내는 것보다, 유효성 검사로 400 에러를 반환하는 게 훨씬 안전하고 명확함.

### 해결: @NotNull + 빈 배열 허용

맛/향 선택은 선택 항목(빈 리스트 허용, null만 금지).

```java
@NotNull(message = "tasteIds는 null일 수 없습니다. 선택하지 않으려면 빈 배열([])을 보내주세요")
private List<Long> tasteIds = new ArrayList<>();
```

| 값 | 결과 |
|----|------|
| `null` | ❌ 400 Bad Request (차단) |
| `[]` | ✅ 정상 처리 (맛/향 없이 저장) |
| `[1, 3]` | ✅ 정상 처리 (선택한 맛/향 저장) |

### @NotNull vs @NotEmpty 차이

| 어노테이션 | null | 빈 리스트 [] |
|-----------|------|------------|
| `@NotNull` | ❌ 차단 | ✅ 허용 |
| `@NotEmpty` | ❌ 차단 | ❌ 차단 |

맛/향이 선택 항목이므로 `@NotNull`이 적절. 필수 항목으로 바꾸고 싶다면 `@NotEmpty`로 교체하면 됨.

---

## HTTP 상태 코드 구분 기준

### 핵심 원칙

```
클라이언트 실수 → 400~409
서버 실수       → 500
```

### 400 Bad Request — 클라이언트가 잘못 보낸 것

| 상황 | 예시 |
|------|------|
| `@NotNull`, `@NotBlank` 등 유효성 검사 실패 | tasteIds에 null 명시 전송 |
| 잘못된 타입 입력 | 카테고리에 "잘못된값" 입력 |
| 비즈니스 규칙 위반 입력 | DRAFT 상태 노트를 isPublic=true로 수정 시도 |
| 이메일/비밀번호 불일치 | 로그인 실패 |
| 중복 가입 | 이미 사용 중인 이메일/닉네임 |

### 401 Unauthorized — 인증 안 된 것

| 상황 | 예시 |
|------|------|
| 유효하지 않은 JWT | 변조된 토큰 |
| 만료된 JWT | 30분 지난 Access Token |

### 403 Forbidden — 인증은 됐는데 권한 없는 것

| 상황 | 예시 |
|------|------|
| 남의 리소스 접근 | 남의 노트 수정/삭제 |
| 허용되지 않은 행위 | 자기 노트 신고 |
| 비공개/DRAFT 노트 타인 조회 | |

### 404 Not Found — 존재하지 않는 리소스

| 상황 | 예시 |
|------|------|
| 없는 유저 ID | |
| 없는 노트 ID | |
| 없는 술 ID | |
| 없는 맛/향 ID | |

### 409 Conflict — 요청은 맞는데 현재 상태와 충돌

| 상황 | 예시 |
|------|------|
| 중복 신고 | 같은 노트 두 번 신고 |
| (추후) 중복 좋아요 | 이미 좋아요한 노트에 또 좋아요 |

### 500 Internal Server Error — 서버 내부 오류

| 상황 | 예시 |
|------|------|
| 예상 못한 모든 예외 | NullPointerException 등 |
| (추후) 외부 서비스 오류 | S3 이미지 업로드 실패 |

### 앞으로 추가될 ErrorCode

기능 구현 시점에 ErrorCode enum에 추가:

| 기능 | ErrorCode | HTTP |
|------|-----------|------|
| AlcoholRequest | `ALCOHOL_REQUEST_NOT_FOUND` | 404 |
| Like | `ALREADY_LIKED` | 409 |
| NoteImage (S3) | `IMAGE_UPLOAD_FAILED` | 500 |

---

## Slack Webhook 알림 (2026-04-07)

### Webhook이란?

특정 이벤트가 발생했을 때 지정한 URL로 HTTP 요청을 자동으로 보내는 방식.
Slack은 채널마다 Webhook URL을 제공하고, 그 URL로 POST 요청을 보내면 채널에 메시지가 와요.

```
서버에서 500 에러 발생
    ↓
NotificationService.sendSlackError() 호출
    ↓
RestTemplate으로 Slack Webhook URL에 HTTP POST
    ↓
Slack 채널에 알림 메시지 도착 🔔
```

---

### `RestTemplate`

Spring에서 외부 HTTP 요청을 보낼 때 쓰는 클래스.

```java
// POST 요청 보내기
restTemplate.postForEntity(url, request, String.class);
```

`@Bean`으로 등록해서 `@RequiredArgsConstructor`로 주입받아 사용:
```java
// AppConfig.java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

**왜 Bean으로 등록하나?**
`new RestTemplate()`을 직접 쓰면 매번 새 객체가 생성됨.
Bean으로 등록하면 하나의 인스턴스를 재사용해서 더 효율적.

---

### `@Slf4j` — 로그 출력 어노테이션

```java
@Slf4j
public class NotificationService {
    log.warn("Slack 알림 전송 실패: {}", ex.getMessage());
}
```

Lombok이 제공하는 어노테이션. `Logger` 객체를 자동으로 생성해줌.
- `log.info()` — 일반 정보
- `log.warn()` — 경고 (심각하지 않은 문제)
- `log.error()` — 에러

Slack 전송 실패 시 서버 에러로 이어지지 않도록 `try-catch`로 감싸고 `log.warn`으로만 기록.

---

### 환경변수로 민감 정보 관리

Webhook URL처럼 외부에 노출되면 안 되는 값은 코드에 직접 쓰면 안 돼요.
GitHub에 올라가면 누구나 볼 수 있기 때문.

```yaml
# application-local.yaml
notification:
  slack-webhook-url: ${SLACK_WEBHOOK_URL:}   # 환경변수에서 읽음, 없으면 빈 값
```

`${변수명:기본값}` 형식 — 환경변수가 없으면 기본값 사용.
`:` 뒤에 아무것도 없으면 빈 문자열이 기본값.

코드에서 읽기:
```java
@Value("${notification.slack-webhook-url:}")
private String slackWebhookUrl;

// URL이 없으면 전송 안 함 (로컬 개발 시 에러 방지)
if (slackWebhookUrl == null || slackWebhookUrl.isBlank()) {
    return;
}
```

IntelliJ Run Configuration → Environment variables에 등록해서 사용.
서버(prod)에서는 GitHub Secrets → 환경변수로 주입.

---

## enum에 필드와 메서드 추가하기 (2026-04-10)

### 왜?
`AlcoholCategory`는 `WHISKEY`, `WINE` 같은 영문 값만 있어서 유저가 "위스키"로 검색하면 결과가 없었어요.
enum에 한글명을 추가해서 해결했어요.

### 구조

```java
@Getter
@RequiredArgsConstructor
public enum AlcoholCategory {
    WHISKEY("위스키"),
    WINE("와인");

    private final String nameKo;  // 생성자로 주입

    public static AlcoholCategory findByNameKo(String keyword) {
        for (AlcoholCategory category : values()) {
            if (category.nameKo.contains(keyword) || keyword.contains(category.nameKo)) {
                return category;
            }
        }
        return null;
    }
}
```

- `@RequiredArgsConstructor` — Lombok이 `final` 필드를 받는 생성자를 자동 생성
- `values()` — enum의 모든 값을 배열로 반환하는 내장 메서드
- `contains` 양방향 체크 — "위스키"가 "위스키 애호가"를 포함하거나, 반대도 매칭

---

## LinkedHashSet으로 중복 없이 순서 유지 (2026-04-10)

### 왜?
검색 결과 두 개(키워드 매칭 + 카테고리 매칭)를 합칠 때 중복이 생길 수 있어요.
예: "조니워커"로 검색 시 nameKo 매칭 + WHISKEY 카테고리 결과 모두 조니워커 포함 가능.

```java
Set<AlcoholResponse> results = new LinkedHashSet<>(keywordResults);
categoryResults.forEach(results::add);  // 중복이면 무시됨
return new ArrayList<>(results);
```

- `HashSet` — 중복 제거되지만 순서 보장 없음
- `LinkedHashSet` — 중복 제거 + 삽입 순서 유지
- `equals/hashCode` 필요 — `@EqualsAndHashCode(of = "id")`로 id 기준 중복 판단

---

## @Validated + @Size로 컨트롤러 파라미터 검증 (2026-04-10)

### 왜?
`@RequestParam`으로 받는 값은 `@Valid`가 아닌 `@Validated`를 클래스에 붙여야 검증이 적용돼요.

```java
@RestController
@Validated  // 클래스에 붙여야 @RequestParam 검증 활성화
public class AlcoholController {

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam @Size(min = 1, message = "검색어는 1자 이상 입력해주세요") String keyword) {
        ...
    }
}
```

- `@Valid` — DTO(RequestBody) 검증에 사용
- `@Validated` — 컨트롤러 클래스에 붙여서 `@RequestParam`, `@PathVariable` 검증 활성화

---

## rating 0.5 단위 검증 (2026-04-10, 11회차에 BigDecimal로 재작성)

### 왜?
`@Min(1)`, `@Max(5)`만으로는 1.3점, 2.7점 같은 잘못된 값을 막을 수 없어요.
0.5 단위인지 검증하는 로직을 서비스에서 직접 처리해야 해요.

### 10회차 — Double + Math.round 방식 (지금은 버려진 접근)

```java
private void validateRating(Double rating) {
    if (Math.round(rating * 10) % 5 != 0) {
        throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
}
```

- 0.5 단위 값을 10배하면 모두 5의 배수: 1.0→10, 1.5→15
- `Math.round()`로 반올림해서 정수 나머지 연산

처음엔 이게 괜찮아 보였는데, 11회차에 다시 보니까 **여전히 뚫리는 값**이 있었어요. 예를 들어 `3.5001`을 보내면:
- `3.5001 * 10 = 35.001...`
- `Math.round(35.001) = 35`
- `35 % 5 == 0` → **통과해버림**

클라이언트가 0.5 단위가 아닌 값을 실수로(혹은 의도적으로) 보낼 때 **간헐적으로 방어가 뚫리는** 상태였고, 반올림 과정에서 "잘못된 입력을 조용히 정상값으로 변환해서 받는" 나쁜 설계이기도 했어요.

### 11회차 — BigDecimal로 전환 (2026-04-12)

```java
private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

private void validateRating(BigDecimal rating) {
    if (rating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) != 0) {
        throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
}
```

**핵심 변화**
- DTO/엔티티의 `rating` 타입을 `Double` → `BigDecimal`로 전환
- 검증도 `BigDecimal.remainder`로 정확히 계산
- `@Min`/`@Max` → `@DecimalMin`/`@DecimalMax`로 변경 (BigDecimal용)
- 허용 범위 0.5 ~ 5.0

**왜 BigDecimal이어야 하나?**
- `Double`은 IEEE 754 부동소수점. `0.1 + 0.2 = 0.30000000000000004` 같은 오차가 발생.
- 별점처럼 **정확한 단위가 있는 수치**(금액, 수량, 점수)는 실무에서 모두 `BigDecimal`을 써요.
- DB 컬럼이 `DECIMAL(2,1)`인데 Java 쪽이 `Double`이면 타입 불일치로 DB 정밀도가 의미 없음. `BigDecimal ↔ DECIMAL`이 자연스러운 대응.
- `BigDecimal("3.5001")`은 값을 문자열 그대로 저장하므로 `remainder(0.5)`가 `0.0001`로 정확히 나와서 거절됨.

**교훈**: "부동소수점으로 정확한 단위 검증하려는 시도 자체가 안티패턴." 타입 선택이 잘못되면 아무리 트릭을 써도 완벽히 방어할 수 없다.

---

## @Pattern으로 입력값 형식 검증 (2026-04-10)

### 언제 쓰나?
`@NotBlank`, `@Size`로 막을 수 없는 형식 조건이 있을 때.

### 닉네임 공백 검증
```java
@Pattern(regexp = "^\\S+$", message = "닉네임에 공백을 포함할 수 없습니다.")
private String nickname;
```
- `^\\S+$` — 처음부터 끝까지 비공백 문자만 허용
- `@NotBlank`는 전체가 공백인 " " 케이스만 차단. "닉 네임" 같은 중간 공백은 통과해버림.

### 비밀번호 복잡도 검증
```java
@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "비밀번호는 영문자와 숫자를 포함해야 합니다.")
private String password;
```
- `(?=.*[A-Za-z])` — 영문자가 최소 하나 이상 포함됨을 확인 (lookahead)
- `(?=.*\\d)` — 숫자가 최소 하나 이상 포함됨을 확인
- `@Size(min=8)`과 함께 쓰면: "8자 이상 + 영문자 + 숫자" 조건 충족

### lookahead(전방탐색)란?
`(?=...)` 는 "이 패턴이 어딘가에 있는지 확인만 하고 위치는 이동하지 않음".
전체 문자열 검사를 여러 조건으로 나눠서 AND 처럼 적용할 수 있음.

---

## 선택적 인증 (Optional Authentication) 패턴 (2026-04-10)

### 언제 필요한가?
같은 API를 로그인/비로그인 유저가 모두 쓸 수 있지만, 로그인 유저에게는 추가 권한을 주고 싶을 때.
예: 노트 상세 조회 — 비로그인도 공개 노트를 볼 수 있지만, 본인 노트는 비공개여도 볼 수 있어야 함.

### 구현 방법

**SecurityConfig**: URL을 permitAll()로 열되, 숫자 ID만 허용해서 /my 같은 다른 경로와 충돌 방지.
```java
.requestMatchers(new RegexRequestMatcher("/api/notes/\\d+", "GET")).permitAll()
```

**Controller**: Authentication이 있으면 userId 추출, 없으면 null.
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
Long userId = (auth != null && auth.getPrincipal() instanceof Long)
        ? (Long) auth.getPrincipal()
        : null;
```

**Service**: null-safe 체크로 소유자 여부 판단.
```java
boolean isOwner = requesterId != null && note.getUser().getId().equals(requesterId);
```

**핵심**: `auth.getPrincipal() instanceof Long` 체크가 중요.
Spring Security는 비로그인 요청에서도 AnonymousAuthenticationToken을 생성하기 때문에,
auth != null 체크만으로는 충분하지 않음. principal 타입이 Long(userId)인지 확인해야 함.

---

## 상태 머신 설계 — 라이프사이클과 공개 의도는 다른 차원 (2026-04-12)

### 배경
노트는 두 가지 "상태"를 가져요.
- `status` (NoteStatus): DRAFT / PUBLISHED — **라이프사이클**
- `isPublic` (boolean): true / false — **공개 의도**

처음에는 이 둘이 비슷해 보여서 "DRAFT면 당연히 비공개지!" 하고 엮었는데, 11회차에 심각한 버그가 있다는 걸 발견했어요.

### 10회차에 저지른 실수
- `createNote()`에서 `isPublic`을 항상 `false`로 고정
- `updateNote()`에서 DRAFT 상태에 `isPublic=true` 설정을 아예 차단
- `publishNote()`는 `status`만 PUBLISHED로 바꾸고 `isPublic`은 건드리지 않음

결과: **공개 노트를 만드는 경로 자체가 존재하지 않았음.** 공개 피드 쿼리가 `status=PUBLISHED AND isPublic=true`인데, 어떤 노트도 이 조건을 만족할 수 없었어요.

### 왜 엮으면 안 되나?
두 필드는 의미가 다른 차원이에요.

| 필드 | 의미 | 언제 바뀌나 |
|---|---|---|
| `status` | 작성이 끝났나? (완성도/라이프사이클) | DRAFT → PUBLISHED (한 방향) |
| `isPublic` | 피드에 노출할 생각인가? (공개 의도) | 언제든 true/false 자유 전환 |

DRAFT이면서 isPublic=true인 상태는 "아직 작성 중이지만 완성하면 공개할 생각" 이라는 **의도(intent)** 를 표현해요. 실제 노출은 `status=PUBLISHED` 조건에서만 일어나므로 안전해요.

### 실제 서비스 사례
- **네이버 블로그 / 티스토리 / 벨로그 / 브런치**: 임시저장 시 공개 설정도 함께 저장
- **Medium**: Draft에 Public/Unlisted 지정 가능
- **Facebook**: Draft에 Audience(공개 범위) 저장
- 공통 패턴: **"Draft + visibility intent"** 를 함께 저장

### 11회차 수정
- `NoteCreateRequest`에 `isPublic` 필드 복원
- `createNote()`에서 하드코딩 제거 → 요청값 사용
- `updateNote()`에서 DRAFT+isPublic=true 금지 제약 제거
- 피드 쿼리는 이미 `status=PUBLISHED AND isPublic=true`라 변경 불필요

### 교훈
**도메인 모델에서 의미가 다른 두 필드를 "비슷해 보인다"고 엮으면 버그가 난다.** 필드 각각에 "이건 뭘 의미하나? 언제 바뀌나? 누가 결정하나?"를 물어봐서 차원이 다르면 독립적으로 다뤄야 함.

---

## TOCTOU (Time-Of-Check to Time-Of-Use) 패턴과 방어 (2026-04-12)

### 개념
**"확인한 시점"과 "사용하는 시점" 사이에 상태가 바뀔 수 있다**는 고전 동시성/보안 버그 패턴.

```java
if (repository.existsBy(...)) {    // ← Check
    throw new BusinessException(...);
}
// ← 이 틈에 다른 요청이 먼저 save 가능
repository.save(...);              // ← Use
```

유닉스 파일 시스템 보안 버그에서 유명해진 용어. "파일 권한 체크" 후 "파일 열기" 사이에 공격자가 심볼릭 링크로 교체하는 공격이 대표적. 보안 면접에서도 키워드로 자주 등장.

### TastingNote 사례 — 중복 신고
Report 생성 로직이 이렇게 되어 있었어요:
```java
if (reportRepository.existsByReporterIdAndNoteId(reporterId, noteId)) {
    throw new BusinessException(ErrorCode.ALREADY_REPORTED);
}
reportRepository.save(newReport);
```

유저가 "신고" 버튼을 더블클릭하거나 네트워크 지연으로 같은 요청이 2번 도착하면:
1. 요청 A: exists → 없음 → 통과
2. 요청 B: exists → 아직 A가 저장 전 → 없음 → 통과
3. 요청 A: save 성공
4. 요청 B: save 성공 ← 중복!

**게다가 Report 엔티티에 unique 제약도 없었음** → 중복이 조용히 DB에 쌓여서 같은 유저가 같은 노트를 여러 번 신고한 걸로 기록될 수 있었어요. 신고 수 부풀리기 어뷰징이 가능한 상태.

### 해결 원칙
TOCTOU의 방어 원칙은 두 가지:
1. **체크와 사용을 원자적(atomic)으로 묶기** — 락(Optimistic/Pessimistic), 트랜잭션 격리 수준 올리기
2. **DB 제약을 최종 방어선으로 두기** — unique, check, foreign key 등

Report는 1번을 적용하면 성능 부담이 너무 커요(락 걸 만큼 중요한 작업도 아님). 그래서 2번 방식을 선택했어요.

### 구현
**엔티티에 unique 제약 추가**:
```java
@Table(
    name = "report",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_report_reporter_note",
        columnNames = {"reporter_id", "note_id"}
    )
)
```

**서비스에서 예외 변환**:
```java
try {
    reportRepository.save(report);
} catch (DataIntegrityViolationException e) {
    throw new BusinessException(ErrorCode.ALREADY_REPORTED);
}
```

### 이중 구조의 의미
- **앱 레벨 `existsBy` 검증**: 정상 케이스(99.99%)에서 빠른 에러 응답 제공 (UX 계층)
- **DB unique 제약**: 동시 요청 시 최종 방어선 (정합성 계층 — source of truth)

DB가 진실의 원천, 서비스 검증은 UX를 위한 빠른 경로. 실무 정석 패턴이에요.

### 교훈
- `existsBy + save` 조합을 볼 때마다 "여기 TOCTOU 아닌가?" 를 의심해야 함.
- DB unique 제약은 "혹시 몰라서" 수준이 아니라 **반드시 설계 단계에서 정해야 하는 도메인 제약**.
- 예외 기반 제어(`try/catch`)는 정상 플로우에는 부담이지만, 동시성 충돌처럼 드물게 발생하는 경쟁 상태에는 적절한 도구.

---

## OAuth 2.0 Refresh Token Rotation + Reuse Detection (2026-04-12)

### 왜 이 패턴이 필요한가?
JWT 인증에서 Access Token은 짧은 수명(예: 1시간), Refresh Token은 긴 수명(예: 30일). Refresh Token이 탈취되면 공격자가 30일 동안 Access Token을 계속 발급받을 수 있어요. **탈취를 빨리 감지하고 즉시 대응하는 메커니즘이 필요**.

### 기존 방식의 한계 (10회차까지)
```java
public TokenResponse reissue(String rtValue) {
    RefreshToken rt = findByToken(rtValue).orElseThrow(...);
    refreshTokenRepository.delete(rt);  // hard delete
    return issueTokens(user);           // 새 토큰 발급
}
```

시나리오:
1. 정상 유저가 RT1 발급받음
2. 공격자가 RT1 탈취 (XSS, 네트워크 가로채기 등)
3. 공격자가 먼저 reissue(RT1) → RT1 삭제, RT2가 공격자에게 발급
4. 정상 유저가 reissue(RT1) → `findByToken(RT1)` → 없음 → `INVALID_TOKEN`
5. 정상 유저는 "어? 로그인 풀렸네" 하고 재로그인
6. 공격자는 그 사이 RT2로 자유롭게 활동 가능

**문제**: 탈취 감지 불가. 정상 유저가 재로그인할 때까지 공격자는 그대로.

### OAuth 2.0 Security BCP 표준 방식
RFC 6819 + draft-ietf-oauth-security-topics가 권장하는 패턴.

**핵심 아이디어**: 이미 한 번 사용(rotated)된 Refresh Token이 또 쓰이면 = 탈취 의심 → 해당 유저의 모든 토큰 즉시 무효화.

**구현 변경점**:
1. `RefreshToken` 엔티티에 `revoked` boolean 필드 추가
2. `reissue()`에서 hard delete 대신 `revoke()` 처리 (삭제 안 하고 흔적 남김)
3. `findByToken`이 revoked 토큰을 찾으면 → **탈취 감지** → 해당 user의 모든 RT 삭제 → 정상/공격자 모두 쫓겨남

```java
public TokenResponse reissue(String rtValue) {
    RefreshToken rt = refreshTokenRepository.findByToken(rtValue)
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

    // 탈취 감지: 이미 revoke된 토큰이 재사용됨
    if (rt.isRevoked()) {
        refreshTokenRepository.deleteByUser(rt.getUser());
        throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }
    if (rt.isExpired()) {
        throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
    }
    User user = rt.getUser();
    if (user.getDeletedAt() != null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    // 정상 회전: 현재 토큰을 revoke (삭제 X, 흔적으로 남김) + 새 토큰 발급
    rt.revoke();
    return issueTokens(user);
}
```

### 공격 시나리오 재실행 (개선 후)
1. 정상 유저 RT1 발급
2. 공격자 RT1 탈취
3. 공격자 reissue(RT1) → RT1 revoked 표시 + RT2 발급 (공격자 보유)
4. 정상 유저 reissue(RT1) → `findByToken(RT1)` → **revoked 발견!** → 해당 user의 모든 RT 삭제(RT2 포함) → `INVALID_TOKEN`
5. 정상 유저는 재로그인 필요하지만, **공격자도 즉시 쫓겨남** (RT2가 함께 삭제됨)

탈취된 토큰이 두 번째로 쓰이는 순간을 탐지 신호로 삼는 게 핵심.

### 책임 분리
기존 `issueTokens()` 내부에서 `deleteByUser()`를 호출했는데, 이를 제거하고 호출자가 명시적으로 정리하도록 바꿨어요.
- `login()`: `deleteByUser` 호출 후 `issueTokens` — clean slate로 시작
- `reissue()` 정상 경로: 개별 토큰 `revoke()`만 하고 `issueTokens` 호출 — 흔적 유지

`issueTokens`가 암묵적으로 DB를 정리하면 reissue 정상 경로에서도 흔적이 지워져서 재사용 감지가 불가능해짐. 책임을 분리해서 각 경로의 정리 정책을 명확히 함.

### 부수 효과와 TODO
revoked 토큰이 DB에 누적되므로 주기적 cleanup이 필요해요.
- `@Scheduled`로 하루 1회 `expires_at < now()` 또는 `revoked = true AND 오래됨` 조건으로 삭제
- context.md "다음 순서 9번"에 TODO로 기록

### 실제 서비스 사례
- **Auth0 / Okta**: Refresh Token Rotation이 기본. reuse detection을 "revocation chain"이라 부름
- **Google OAuth**: 비슷한 개념을 암묵적으로 적용
- **AWS Cognito**: `EnableTokenRevocation` 옵션으로 활성화

### 교훈
- 보안은 "뚫리는지"보다 "**뚫렸을 때 얼마나 빨리 감지하고 대응할 수 있는지**"가 더 중요.
- Hard delete는 편하지만 "흔적이 남지 않는다"는 단점이 있음. 감사/탐지 목적일 땐 soft delete가 더 강력.
- OAuth 2.0 Security BCP처럼 **업계 표준 패턴**이 있는 영역은 자기류로 만들지 말고 표준을 그대로 따르는 게 안전. 표준은 수많은 공격 시나리오를 반영한 결과물.

---

## BigDecimal vs Double — 정확한 수치는 정확한 타입으로 (2026-04-12)

### 언제 BigDecimal을 써야 하나?
**"단위가 정해진 정확한 수치"** 를 다룰 때. 대표적으로:
- 금액(원, 달러)
- 수량(개, 인원)
- 점수/별점(0.5 단위)
- 백분율, 세율 등

반대로 **연속적인 측정값**(거리, 속도, 온도, 확률 등)은 `Double`로 충분해요. 약간의 오차가 큰 문제가 안 되니까.

### Double의 함정
`Double`은 IEEE 754 부동소수점. 유한한 비트로 무한한 실수를 표현하려다 보니 오차가 불가피해요.
```java
System.out.println(0.1 + 0.2);  // 0.30000000000000004
System.out.println(3.5001 * 10); // 35.001000000000005
```
`1.5`라는 값조차 내부적으로는 정확히 저장되지 않아요. 그래서 `==` 비교, 나머지 연산, 누적 덧셈 같은 연산에서 직관과 어긋나는 결과가 나옴.

### BigDecimal의 원리
값을 `unscaled value (정수) + scale (10의 몇 제곱)` 형태로 저장해요.
- `new BigDecimal("1.5")` → unscaledValue=15, scale=1 → 값 = 15 × 10⁻¹ = 1.5 (정확)
- 생성 시 **반드시 `String` 생성자를 써야** 함. `new BigDecimal(1.5)`(Double 생성자)는 이미 Double 오차가 들어간 값을 받아오니까 의미 없음.

### DB 타입 대응
```
Java BigDecimal  ↔  SQL DECIMAL / NUMERIC
Java Double      ↔  SQL DOUBLE / REAL
```
DB 컬럼이 `DECIMAL(2,1)`이면 Java 필드도 `BigDecimal`이어야 정밀도가 유지돼요. `Double`로 받으면 JPA가 내부 변환을 하면서 오차가 생길 수 있음.

### TastingNote 적용 사례
- `Note.rating` — 0.5 단위 별점
- 검증: `rating.remainder(new BigDecimal("0.5")).compareTo(BigDecimal.ZERO) != 0`
- Bean Validation: `@DecimalMin("0.5")`, `@DecimalMax("5.0")`

### 주의할 것들
- **비교는 `compareTo()` 써라.** `equals()`는 scale도 비교하므로 `new BigDecimal("1.5")`와 `new BigDecimal("1.50")`이 다른 값으로 판정됨. 값만 비교하려면 `compareTo() == 0`.
- **연산 후 결과를 재할당**해야 함. `BigDecimal`은 immutable. `a.add(b)`는 a를 바꾸는 게 아니라 새 객체를 반환.
- **`divide()`는 나누어떨어지지 않으면 `ArithmeticException`** 발생. `divide(divisor, scale, RoundingMode)` 형태로 명시해야 안전.

### 교훈
- 금액/점수/수량은 **무조건 BigDecimal**. 처음부터 그렇게 쓰는 습관이 중요.
- 타입 선택이 잘못되면 나중에 어떤 트릭을 써도 근본적으로 해결되지 않음 (rating 0.5 단위 검증이 딱 이 사례).

---

## 역할 기반 접근 제어 (RBAC) — Spring Security hasRole (13회차)

### 개념
RBAC(Role-Based Access Control) — "이 API는 ADMIN만 쓸 수 있다"처럼 역할에 따라 접근을 제한하는 방식.

### GrantedAuthority / SimpleGrantedAuthority
Spring Security에서 "이 유저는 어떤 역할을 가지고 있다"를 표현하는 인터페이스와 구현체.

```java
// JwtAuthenticationFilter에서 role을 authority로 변환
UserRole role = jwtTokenProvider.getUserRole(token);
List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

// UsernamePasswordAuthenticationToken 3번째 인수가 authorities
// 이전: Collections.emptyList() → authority 없음 (hasRole이 항상 false)
// 이후: authorities 전달 → ROLE_USER / ROLE_ADMIN 구분 가능
new UsernamePasswordAuthenticationToken(userId, null, authorities);
```

**`ROLE_` 접두사가 왜 필요한가?**
`hasRole("ADMIN")` 메서드는 내부적으로 `ROLE_ADMIN`을 찾음. Spring Security 관례상 역할명에는 `ROLE_` 접두사가 붙어야 함. `hasAuthority("ADMIN")`은 접두사 없이 직접 매칭.

### SecurityConfig에서 역할 제한
```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")  // ROLE_ADMIN만 접근 가능
// 이 설정이 없으면 authenticated()로만 걸러져서 USER도 /api/admin/** 호출 가능
```

### JWT에 role 클레임 추가
```java
// 발급 시
Jwts.builder()
    .claim("role", role.name())  // "USER" or "ADMIN"
    ...

// 추출 시
String roleName = getClaims(token).get("role", String.class);
UserRole.valueOf(roleName);
```
토큰 안에 role을 담아두면 매 요청마다 DB에서 유저 역할을 조회하지 않아도 됨. Stateless 방식의 핵심 이점.

---

## @ElementCollection — 단순 값 컬렉션을 별도 테이블로 관리

### 개념
엔티티가 아닌 단순 값(String, Integer 등)의 리스트를 DB에 저장할 때 사용.

```java
// AlcoholRequest.java
@ElementCollection
@CollectionTable(name = "alcohol_request_alias", joinColumns = @JoinColumn(name = "request_id"))
@Column(name = "alias")
@Builder.Default
private List<String> aliases = new ArrayList<>();
```

### 생성되는 DB 구조
```
alcohol_request_alias 테이블
- request_id (FK → alcohol_request.id)
- alias (VARCHAR)
```
별칭 하나가 행 하나. AlcoholRequest 10개에 별칭 3개씩이면 30행.

### `@ElementCollection` vs `@OneToMany`
| | `@ElementCollection` | `@OneToMany` |
|---|---|---|
| 저장 대상 | 단순 값 (String 등) | 엔티티 객체 |
| 독립 조회 | 불가 (부모 없이 못 씀) | 가능 |
| 코드 복잡도 | 낮음 | 높음 |

별칭처럼 "이 요청의 별칭" 이상의 의미가 없는 단순 값이면 `@ElementCollection`이 적합.

### `@Builder.Default` 와 컬렉션
Lombok `@Builder`는 기본값을 무시하고 null로 초기화하는 문제가 있음. 컬렉션 필드에 `@Builder.Default`를 붙여야 `new ArrayList<>()`가 적용됨.
```java
// 없으면: builder().build() → aliases = null → NullPointerException 위험
// 있으면: builder().build() → aliases = [] (빈 리스트)
@Builder.Default
private List<String> aliases = new ArrayList<>();
```
- DB 설계 단계에서 `DECIMAL`로 정했으면 Java 필드도 자동으로 `BigDecimal`이 되어야 자연스러움.

### @ElementCollection의 LazyInitializationException (13회차)
`@ElementCollection`의 기본 fetch 타입은 **LAZY**. 엔티티를 조회한 트랜잭션이 닫힌 뒤에 컬렉션에 접근하면 예외 발생.

```
org.hibernate.LazyInitializationException: failed to lazily initialize a collection
```

**원인**: Service 메서드에 `@Transactional`이 있어도, 응답 직렬화(JSON 변환)는 트랜잭션 밖에서 발생함. DTO 변환 시 `aliases`를 참조하면 이미 세션이 닫혀 있음.

**해결 방법 두 가지**:
```java
// A안: EAGER로 변경 (aliases가 항상 필요한 경우 — AlcoholRequest가 이 케이스)
@ElementCollection(fetch = FetchType.EAGER)
private List<String> aliases = new ArrayList<>();

// B안: DTO 변환을 @Transactional 메서드 안에서 처리 (LAZY 유지)
// — Service에서 aliases.stream().toList() 등 실제 접근을 트랜잭션 안에서 완료
```

`AlcoholRequest`에서는 응답마다 aliases가 필요하므로 A안(EAGER) 선택. aliases가 가끔만 필요하고 크기가 클 경우엔 B안이 더 효율적.

---

## AccessDeniedHandler — 권한 없는 접근 시 JSON 응답 (13회차)

### 문제
`SecurityConfig`에서 `.requestMatchers("/api/admin/**").hasRole("ADMIN")` 설정 후, USER 권한 유저가 `/api/admin/**`에 접근하면 Spring Security가 기본 동작으로 빈 403 응답을 반환.
프론트 입장에서 빈 바디의 403은 파싱 불가 → 에러 처리 어려움.

### 해결: AccessDeniedHandler 구현
```java
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // ErrorResponse JSON 직접 작성
        response.getWriter().write(...);
    }
}
```

`SecurityConfig`에 등록:
```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint(customAuthenticationEntryPoint)  // 401: 미인증
    .accessDeniedHandler(customAccessDeniedHandler)            // 403: 인증됐지만 권한 없음
)
```

### AuthenticationEntryPoint vs AccessDeniedHandler 차이
| | 언제 호출되나 | HTTP 상태 |
|---|---|---|
| `AuthenticationEntryPoint` | 토큰 없음 / 토큰 위변조 / 만료 | 401 Unauthorized |
| `AccessDeniedHandler` | 토큰 있지만 역할(role)이 부족 | 403 Forbidden |

USER 토큰으로 ADMIN API 접근 → 인증은 됐지만 권한 없음 → `AccessDeniedHandler` 호출.
