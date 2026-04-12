package com.dongjin.tastingnote.user.service;

import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
import com.dongjin.tastingnote.common.jwt.JwtTokenProvider;
import com.dongjin.tastingnote.user.dto.LoginRequest;
import com.dongjin.tastingnote.user.dto.SignUpRequest;
import com.dongjin.tastingnote.user.dto.TokenResponse;
import com.dongjin.tastingnote.user.entity.Provider;
import com.dongjin.tastingnote.user.entity.RefreshToken;
import com.dongjin.tastingnote.user.entity.User;
import com.dongjin.tastingnote.user.repository.RefreshTokenRepository;
import com.dongjin.tastingnote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public void signUp(SignUpRequest request) {
        String email = request.getEmail().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByNicknameAndDeletedAtIsNull(request.getNickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .birthDate(request.getBirthDate())
                .provider(Provider.LOCAL)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        // 로그인은 깨끗한 상태에서 시작: 이전 토큰(유효/만료/revoked 모두) 전부 정리
        refreshTokenRepository.deleteByUser(user);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse reissue(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        // OAuth 2.0 Security BCP: Refresh Token Reuse Detection
        // 이미 회전(revoked)된 토큰이 재사용되면 탈취 의심 → 해당 유저의 모든 토큰 무효화
        if (refreshToken.isRevoked()) {
            refreshTokenRepository.deleteByUser(refreshToken.getUser());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        if (refreshToken.isExpired()) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }

        User user = refreshToken.getUser();
        if (user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 정상 회전: 기존 토큰은 삭제하지 않고 revoked 처리(재사용 감지를 위한 흔적)
        refreshToken.revoke();
        return issueTokens(user);
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        refreshTokenRepository.deleteByUser(user);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshTokenValue);
    }
}