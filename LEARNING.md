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

## 앞으로 추가 예정

- `SecurityConfig` — JwtAuthenticationFilter 등록, 인증 필요/불필요 URL 구분
- `UserRepository` — 이메일로 유저 조회
- `UserService` — 회원가입, 로그인, 토큰 재발급
- `UserController` — API 엔드포인트