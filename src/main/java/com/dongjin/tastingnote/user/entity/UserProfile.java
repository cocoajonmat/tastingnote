package com.dongjin.tastingnote.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String profileJson;

    private LocalDateTime updatedAt;

    public static UserProfile of(Long userId, String profileJson) {
        UserProfile profile = new UserProfile();
        profile.userId = userId;
        profile.profileJson = profileJson;
        profile.updatedAt = LocalDateTime.now();
        return profile;
    }

    public void update(String profileJson) {
        this.profileJson = profileJson;
        this.updatedAt = LocalDateTime.now();
    }
}