package com.dongjin.tastingnote.user.dto;

import com.dongjin.tastingnote.user.entity.User;
import com.dongjin.tastingnote.user.entity.UserRole;

public record UserInfoResponse(
        String nickname,
        String email,
        String profileImageUrl,
        UserRole role
) {
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getNickname(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getRole()
        );
    }
}
