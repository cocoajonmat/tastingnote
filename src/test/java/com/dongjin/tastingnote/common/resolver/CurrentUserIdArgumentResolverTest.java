package com.dongjin.tastingnote.common.resolver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CurrentUserIdArgumentResolver 단위 테스트
 *
 * 이 클래스는 Spring 컨텍스트나 Mockito 없이 테스트 가능.
 * → SecurityContextHolder는 static 메서드이므로 직접 조작 가능.
 */
class CurrentUserIdArgumentResolverTest {

    private CurrentUserIdArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CurrentUserIdArgumentResolver();
        // 각 테스트 시작 전 SecurityContext 초기화 (테스트 간 간섭 방지)
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 종료 후에도 SecurityContext 정리
        // → 테스트가 전역 상태(SecurityContextHolder)를 변경하므로
        //   다른 테스트에 영향을 주지 않도록 반드시 초기화해야 함
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증된 사용자의 경우 userId(Long)를 반환한다")
    void resolveArgument_whenAuthenticated_returnsUserId() {
        // given: SecurityContext에 userId=42를 담은 인증 정보 설정
        // JwtAuthenticationFilter가 로그인 시 이렇게 SecurityContext에 저장함
        Long userId = 42L;
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when: resolver가 파라미터 값을 결정
        // resolveArgument()의 파라미터들은 컨트롤러 메서드 파라미터 정보인데,
        // 이 resolver는 SecurityContext만 보고 판단하므로 null로 전달해도 됨
        Object result = resolver.resolveArgument(null, null, null, null);

        // then
        assertThat(result).isEqualTo(userId);
    }

    @Test
    @DisplayName("인증 정보가 없으면(비로그인) null을 반환한다")
    void resolveArgument_whenNotAuthenticated_returnsNull() {
        // given: SecurityContext가 비어있음 (setUp에서 clearContext 호출됨)

        // when
        Object result = resolver.resolveArgument(null, null, null, null);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("principal이 Long이 아니면(익명 인증) null을 반환한다")
    void resolveArgument_whenPrincipalIsNotLong_returnsNull() {
        // given: principal이 String인 경우 (Spring Security 익명 인증의 경우 "anonymousUser" 문자열)
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // when
        Object result = resolver.resolveArgument(null, null, null, null);

        // then: Long이 아니므로 null 반환
        assertThat(result).isNull();
    }
}