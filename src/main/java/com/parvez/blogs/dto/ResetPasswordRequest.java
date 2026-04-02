package com.parvez.blogs.dto;

public record ResetPasswordRequest(String resetToken, String newPassword) {
}