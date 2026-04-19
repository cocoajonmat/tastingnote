package com.dongjin.tastingnote.user.service;

import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
import com.dongjin.tastingnote.common.jwt.JwtTokenProvider;
import com.dongjin.tastingnote.common.s3.S3Port;
import com.dongjin.tastingnote.user.dto.*;
import com.dongjin.tastingnote.user.entity.Provider;
import com.dongjin.tastingnote.user.entity.User;
import com.dongjin.tastingnote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final S3Port s3Port;

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

        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse reissue(String refreshTokenValue) {
        Long userId = jwtTokenProvider.validateAndGetUserIdFromRefreshToken(refreshTokenValue);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return issueTokens(user);
    }

    public void logout(Long userId) {
        // Stateless: 클라이언트가 토큰을 폐기하면 됨.
        // 강제 로그아웃이 필요하면 추후 Redis 블랙리스트로 추가 예정.
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserInfoResponse.from(user);
    }

    @Transactional(readOnly = true)
    public void checkNicknameAvailable(String nickname) {
        if (userRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }

    @Transactional
    public void updateNickname(Long userId, UpdateNicknameRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.getNickname().equals(request.nickname())
                && userRepository.existsByNicknameAndDeletedAtIsNull(request.nickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        user.updateNickname(request.nickname());
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public ProfileImageResponse updateProfileImage(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        long maxSize = 20 * 1024 * 1024; // 20MB
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_TOO_LARGE);
        }

        // todo: S3 key를 DB에 별도 저장하도록 개선 필요 (현재는 URL에서 역추출)
        if (user.getProfileImageUrl() != null) {
            String oldKey = extractS3Key(user.getProfileImageUrl());
            s3Port.delete(oldKey);
        }

        String key = "profile/" + userId + "/" + UUID.randomUUID();
        String imageUrl = s3Port.upload(file, key);
        user.updateProfileImageUrl(imageUrl);
        return new ProfileImageResponse(imageUrl);
    }

    private String extractS3Key(String url) {
        return url.substring(url.indexOf(".com/") + 5);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        return new TokenResponse(accessToken, refreshToken);
    }
}
