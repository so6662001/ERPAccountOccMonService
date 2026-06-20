package com.puxun.monitor.security.support;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具。
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static AuthUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser u) {
            return u;
        }
        return null;
    }

    public static String currentUsername() {
        AuthUser u = currentUser();
        return u == null ? null : u.getUsername();
    }

    public static String currentUsernameOrSystem() {
        String u = currentUsername();
        return u == null ? "system" : u;
    }

    public static boolean hasRole(String roleCode) {
        AuthUser u = currentUser();
        return u != null && u.getRoles().contains(roleCode);
    }
}
