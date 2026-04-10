package com.dongjin.tastingnote.user.controller;

import com.dongjin.tastingnote.common.response.ApiResponse;
import com.dongjin.tastingnote.user.dto.LoginRequest;
import com.dongjin.tastingnote.user.dto.SignUpRequest;
import com.dongjin.tastingnote.user.dto.TokenResponse;
import com.dongjin.tastingnote.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "회원가입, 로그인, 토큰 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 신규 계정을 생성합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest request) {
        userService.signUp(request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다. Access Token과 Refresh Token을 반환합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = userService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(tokenResponse));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새로운 Access Token을 발급받습니다. 헤더에 Refresh-Token을 담아 요청하세요.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@RequestHeader("Refresh-Token") String refreshToken) {
        TokenResponse tokenResponse = userService.reissue(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(tokenResponse));
    }

    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자를 로그아웃합니다. Refresh Token이 삭제됩니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }



}