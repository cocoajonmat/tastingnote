package com.dongjin.tastingnote.oauth.service;

import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
import com.dongjin.tastingnote.common.jwt.JwtTokenProvider;
import com.dongjin.tastingnote.oauth.client.OAuthClient;
import com.dongjin.tastingnote.oauth.dto.OAuthLoginRequest;
import com.dongjin.tastingnote.oauth.dto.OAuthLoginResponse;
import com.dongjin.tastingnote.oauth.dto.OAuthUserInfo;
import com.dongjin.tastingnote.user.entity.Provider;
import com.dongjin.tastingnote.user.entity.User;
import com.dongjin.tastingnote.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OAuthService {

    private final Map<Provider, OAuthClient> clientMap;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public OAuthService(List<OAuthClient> clients, UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.clientMap = clients.stream().collect(Collectors.toMap(OAuthClient::provider, Function.identity()));
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public OAuthLoginResponse login(String providerStr, OAuthLoginRequest request) {
        Provider provider = parseProvider(providerStr);
        OAuthClient client = clientMap.get(provider);
        if (client == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_PROVIDER);
        }

        OAuthUserInfo userInfo = client.fetchUserInfo(request.code(), request.redirectUri());

        if (userInfo.email() == null) {
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_REQUIRED);
        }

        Optional<User> existing = userRepository.findByProviderAndProviderId(provider, userInfo.providerId());
        if (existing.isPresent()) {
            return issueResponse(existing.get(), false);
        }

        if (userRepository.existsByEmail(userInfo.email())) {
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_CONFLICT);
        }

        String tempNickname = generateUniqueNickname(provider);

        User newUser = User.builder()
                .email(userInfo.email())
                .nickname(tempNickname)
                .profileImageUrl(userInfo.profileImageUrl())
                .provider(provider)
                .providerId(userInfo.providerId())
                .build();

        userRepository.save(newUser);
        return issueResponse(newUser, true);
    }

    private Provider parseProvider(String providerStr) {
        try {
            return Provider.valueOf(providerStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_PROVIDER);
        }
    }

    private String generateUniqueNickname(Provider provider) {
        String prefix = provider.name().toLowerCase() + "_";
        for (int i = 0; i < 3; i++) {
            String nickname = prefix + UUID.randomUUID().toString().substring(0, 8);
            if (!userRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
                return nickname;
            }
        }
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private OAuthLoginResponse issueResponse(User user, boolean isNewUser) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        return new OAuthLoginResponse(accessToken, refreshToken, isNewUser);
    }
}