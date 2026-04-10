package com.parvez.blogs.controller;

import com.parvez.blogs.config.ApiPaths;
import com.parvez.blogs.dto.ForgotPasswordRequest;
import com.parvez.blogs.dto.ResetPasswordRequest;
import com.parvez.blogs.service.CredentialService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.AUTH)
@RequiredArgsConstructor
public class CredentialController {

    private final CredentialService credentialService;


    @Transactional
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotpassword(@Valid @RequestBody ForgotPasswordRequest requestDTO) {
        credentialService.forgotPassword(requestDTO.email());
        return ResponseEntity.ok().build();
    }

    @Transactional
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetpassword(@Valid @RequestBody ResetPasswordRequest requestDTO) {
        credentialService.resetPassword(requestDTO.newPassword(), requestDTO.resetToken());
        return ResponseEntity.ok().build();
    }
}
