package com.dongjin.tastingnote.user.repository;

import com.dongjin.tastingnote.user.entity.Provider;
import com.dongjin.tastingnote.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmail(String email);

    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}