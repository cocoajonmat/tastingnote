package com.dongjin.tastingnote.user.controller;

import com.dongjin.tastingnote.user.dto.LoginRequest;
import com.dongjin.tastingnote.user.dto.SignUpRequest;
import com.dongjin.tastingnote.user.dto.TokenResponse;
import com.dongjin.tastingnote.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@Valid @RequestBody SignUpRequest request) {
        userService.signUp(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = userService.login(request);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestHeader("Refresh-Token") String refreshToken) {
        TokenResponse tokenResponse = userService.reissue(refreshToken);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.logout(userId);
        return ResponseEntity.ok().build();
    }
}