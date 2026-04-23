package com.dongjin.tastingnote.oauth.controller;

import com.dongjin.tastingnote.oauth.dto.OAuthLoginRequest;
import com.dongjin.tastingnote.oauth.dto.OAuthLoginResponse;
import com.dongjin.tastingnote.oauth.service.OAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/oauth")
public class OAuthController {

    private final OAuthService oAuthService;

    public OAuthController(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    @PostMapping("/{provider}")
    public ResponseEntity<OAuthLoginResponse> login(
            @PathVariable String provider,
            @RequestBody @Valid OAuthLoginRequest request
    ) {
        return ResponseEntity.ok(oAuthService.login(provider, request));
    }
}