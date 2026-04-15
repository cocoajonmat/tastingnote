package com.dongjin.tastingnote.common.jwt;

import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
import com.dongjin.tastingnote.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 단위 테스트
 *
 * 단위 테스트란?
 * → 클래스 하나를 외부 의존 없이 독립적으로 테스트하는 것.
 * → JwtTokenProvider는 Repository나 Spring 컨텍스트가 전혀 필요 없어서
 *   가장 단순한 형태의 단위 테스트를 작성할 수 있음.
 *
 * @SpringBootTest 없이 그냥 new 로 직접 생성해서 테스트함.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    // 테스트용 고정 값들
    // JWT secret은 HS256 기준 최소 32바이트(256bit) 이상이어야 함
    private static final String TEST_SECRET = "test-secret-key-must-be-at-least-32-bytes-long!!";
    private static final long ACCESS_EXPIRATION = 1000 * 60 * 60L;  // 1시간 (ms 단위)
    private static final long REFRESH_EXPIRATION = 1000 * 60 * 60 * 24 * 30L;  // 30일

    /**
     * @BeforeEach
     * → 각 @Test 메서드가 실행되기 직전에 자동으로 호출됨.
     * → 테스트마다 깨끗한 객체를 새로 만들어서 테스트 간 상태가 섞이지 않게 함.
     */
    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(TEST_SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
    }

    /**
     * @Test: 이 메서드가 테스트임을 JUnit에게 알려줌
     * @DisplayName: 테스트 결과 화면에 표시될 이름. 한글로 써도 됨.
     *
     * 테스트 구조 — "given / when / then" (또는 AAA: Arrange / Act / Assert)
     * - given: 테스트에 필요한 데이터 준비
     * - when: 실제로 테스트할 동작 실행
     * - then: 결과 검증
     */
    @Test
    @DisplayName("Access Token을 생성하면 userId를 다시 꺼낼 수 있다")
    void generateAccessToken_thenGetUserIdReturnsCorrect() {
        // given
        Long userId = 42L;

        // when
        String token = tokenProvider.generateAccessToken(userId, UserRole.USER);

        // then
        assertThat(tokenProvider.getUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("Access Token에 포함된 role을 getUserRole로 꺼낼 수 있다")
    void generateAccessToken_withAdminRole_thenGetUserRoleReturnsAdmin() {
        // given
        String token = tokenProvider.generateAccessToken(1L, UserRole.ADMIN);

        // when & then
        assertThat(tokenProvider.getUserRole(token)).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("USER role로 발급한 토큰에서 getUserRole은 USER를 반환한다")
    void generateAccessToken_withUserRole_thenGetUserRoleReturnsUser() {
        // given
        String token = tokenProvider.generateAccessToken(1L, UserRole.USER);

        // when & then
        assertThat(tokenProvider.getUserRole(token)).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("유효한 토큰은 validateToken이 true를 반환한다")
    void validateToken_withValidToken_returnsTrue() {
        // given
        String token = tokenProvider.generateAccessToken(1L, UserRole.USER);

        // when & then
        assertThat(tokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("변조된 토큰은 validateToken이 false를 반환한다")
    void validateToken_withTamperedToken_returnsFalse() {
        // given: 정상 토큰을 생성한 뒤 뒤에 문자를 붙여서 위조
        String validToken = tokenProvider.generateAccessToken(1L, UserRole.USER);
        String tamperedToken = validToken + "tampered";

        // when & then
        assertThat(tokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("완전히 잘못된 문자열은 validateToken이 false를 반환한다")
    void validateToken_withGarbageToken_returnsFalse() {
        assertThat(tokenProvider.validateToken("this.is.not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("유효한 Refresh Token에서 userId를 꺼낼 수 있다")
    void validateAndGetUserIdFromRefreshToken_withValidToken_returnsUserId() {
        // given
        Long userId = 7L;
        String refreshToken = tokenProvider.generateRefreshToken(userId);

        // when
        Long result = tokenProvider.validateAndGetUserIdFromRefreshToken(refreshToken);

        // then
        assertThat(result).isEqualTo(userId);
    }

    @Test
    @DisplayName("만료된 Refresh Token은 EXPIRED_TOKEN 예외를 던진다")
    void validateAndGetUserIdFromRefreshToken_withExpiredToken_throwsExpiredToken() {
        // given: 만료시간을 -1ms로 설정 → 생성 즉시 이미 만료된 토큰
        JwtTokenProvider expiredProvider = new JwtTokenProvider(TEST_SECRET, -1L, -1L);
        String expiredToken = expiredProvider.generateRefreshToken(1L);

        // then
        // assertThatThrownBy: 예외가 던져지는지 검증하는 AssertJ 메서드
        // .isInstanceOf: 예외 타입 확인
        // .extracting: 예외 객체에서 특정 필드 값을 꺼냄
        // .isEqualTo: 그 값이 맞는지 확인
        assertThatThrownBy(() -> tokenProvider.validateAndGetUserIdFromRefreshToken(expiredToken))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPIRED_TOKEN);
    }

    @Test
    @DisplayName("변조된 Refresh Token은 INVALID_TOKEN 예외를 던진다")
    void validateAndGetUserIdFromRefreshToken_withTamperedToken_throwsInvalidToken() {
        // given
        String validToken = tokenProvider.generateRefreshToken(1L);
        String tamperedToken = validToken + "hacked";

        // then
        assertThatThrownBy(() -> tokenProvider.validateAndGetUserIdFromRefreshToken(tamperedToken))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }
}