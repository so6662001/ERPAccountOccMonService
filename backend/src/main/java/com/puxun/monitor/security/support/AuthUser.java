package com.puxun.monitor.security.support;

import lombok.Getter;

import java.util.List;

/**
 * 认证主体，作为 Authentication 的 principal。
 */
@Getter
public class AuthUser {

    private final Long userId;
    private final String username;
    private final List<String> roles;
    private final List<String> permissions;

    public AuthUser(Long userId, String username, List<String> roles, List<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
        this.permissions = permissions;
    }
}
