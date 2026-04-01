package com.dongjin.tastingnote.user.service;

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
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .birthDate(request.getBirthDate())
                .provider(Provider.LOCAL)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse reissue(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("만료된 토큰입니다. 다시 로그인해주세요.");
        }

        refreshTokenRepository.delete(refreshToken);
        return issueTokens(refreshToken.getUser());
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        refreshTokenRepository.deleteByUser(user);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getId());

        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshTokenValue);
    }
}