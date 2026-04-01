# 개념 학습 노트

개발하면서 새로 접한 개념들을 정리하는 파일입니다.
Claude Code가 새로운 클래스/기능을 만들 때마다 여기에 설명을 추가합니다.

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
| **저장 위치** | 클라이언트 (메모리/로컬스토리지) | 서버 DB |
| **왜 짧게?** | 탈취 시 피해 최소화 | 길어야 재로그인 안 해도 됨 |

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
- **`reissue`** — Refresh Token 검증 후 기존 토큰 삭제하고 새 토큰 발급 (재사용 방지).
- **`logout`** — DB에서 Refresh Token 삭제. 이후 재발급 불가.
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
- **`SecurityContextHolder.getContext().getAuthentication().getPrincipal()`** — JwtAuthenticationFilter에서 저장한 userId를 꺼내는 코드. 인증이 필요한 API에서 현재 로그인한 유저를 이렇게 가져옴.

---

## 앞으로 추가 예정

- `AlcoholService/Controller` — 술 검색 (name + nameKo + alias 통합)
- `TagService/Controller` — 태그 자동완성, NoteTag 연결
- `LikeService/Controller` — 반응 기능
- `NoteController` userId → JWT에서 추출로 변경