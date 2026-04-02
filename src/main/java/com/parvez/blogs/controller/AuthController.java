package com.parvez.blogs.controller;

import com.parvez.blogs.config.ApiPaths;
import com.parvez.blogs.dto.*;
import com.parvez.blogs.repository.RefreshTokenRepository;
import com.parvez.blogs.security.JwtUtil;
import com.parvez.blogs.service.AuthService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.AUTH)
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    @PostMapping(ApiPaths.REGISTER)
    public ResponseEntity<UserResponse> register(@RequestBody UserRegister dto) {
        return ResponseEntity.ok(authService.register(dto));
    }

    @PostMapping(ApiPaths.LOGIN)
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @Transactional
    @PostMapping(ApiPaths.REFRESH_TOKEN)
    public ResponseEntity<TokenResponse> refresh(
             @Valid @RequestBody RefreshRequest request
    ) {
        TokenResponse response = authService.refreshRotation(request.refreshToken());
        return ResponseEntity.ok(response);
    }

}
