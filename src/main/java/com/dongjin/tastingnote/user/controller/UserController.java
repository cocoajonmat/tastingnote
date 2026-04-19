package com.dongjin.tastingnote.user.controller;

import com.dongjin.tastingnote.common.resolver.CurrentUserId;
import com.dongjin.tastingnote.user.dto.*;
import com.dongjin.tastingnote.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "사용자", description = "회원가입, 로그인, 토큰, 프로필 수정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/")
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 신규 계정을 생성합니다.")
    @PostMapping("signup")
    public ResponseEntity<Void> signUp(@Valid @RequestBody SignUpRequest request) {
        userService.signUp(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다. Access Token과 Refresh Token을 반환합니다.")
    @PostMapping("login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새로운 Access Token을 발급받습니다. 헤더에 Refresh-Token을 담아 요청하세요.")
    @PostMapping("reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(userService.reissue(refreshToken));
    }

    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자를 로그아웃합니다. 클라이언트가 토큰을 폐기하면 됩니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("logout")
    public ResponseEntity<Void> logout(@CurrentUserId Long userId) {
        userService.logout(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 정보를 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("me")
    public ResponseEntity<UserInfoResponse> getMyInfo(@CurrentUserId Long userId) {
        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

    @Operation(summary = "닉네임 중복 확인", description = "해당 닉네임이 사용 가능한지 확인합니다. 사용 가능하면 200, 이미 존재하면 409를 반환합니다.")
    @GetMapping("check-nickname")
    public ResponseEntity<Void> checkNickname(@RequestParam String nickname) {
        userService.checkNicknameAvailable(nickname);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "닉네임 수정", description = "현재 로그인된 사용자의 닉네임을 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("me/nickname")
    public ResponseEntity<Void> updateNickname(
            @CurrentUserId Long userId,
            @Valid @RequestBody UpdateNicknameRequest request) {
        userService.updateNickname(userId, request);
		return ResponseEntity.ok().build();
    }

    @Operation(summary = "비밀번호 수정", description = "현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("me/password")
    public ResponseEntity<Void> updatePassword(
            @CurrentUserId Long userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(userId, request);
		return ResponseEntity.ok().build();
    }

    @Operation(summary = "프로필 이미지 수정", description = "프로필 이미지를 업로드하여 변경합니다. 기존 이미지가 있으면 삭제 후 교체됩니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping(value = "me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileImageResponse> updateProfileImage(
            @CurrentUserId Long userId,
            @RequestPart("file") MultipartFile file) {
        ProfileImageResponse response = userService.updateProfileImage(userId, file);
        return ResponseEntity.ok(response);
    }
}
