package com.puxun.monitor.security.auth.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 认证相关 DTO 集合。
 */
public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record TokenResponse(String accessToken, String refreshToken, long expiresInMinutes) {}

    public record UserInfo(Long userId, String username, String displayName,
                           List<String> roles, List<String> permissions) {}
}
