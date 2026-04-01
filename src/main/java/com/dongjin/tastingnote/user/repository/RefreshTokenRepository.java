package com.dongjin.tastingnote.user.repository;

import com.dongjin.tastingnote.user.entity.RefreshToken;
import com.dongjin.tastingnote.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}