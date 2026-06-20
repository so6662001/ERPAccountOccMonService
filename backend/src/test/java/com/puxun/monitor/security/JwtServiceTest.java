package com.puxun.monitor.security;

import com.puxun.monitor.security.config.PlatformSecurityProperties;
import com.puxun.monitor.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService newService() {
        PlatformSecurityProperties props = new PlatformSecurityProperties();
        props.getJwt().setSecret("unit-test-secret-key-which-is-long-enough-32bytes!!");
        props.getJwt().setAccessTtlMinutes(60);
        props.getJwt().setRefreshTtlDays(1);
        return new JwtService(props);
    }

    @Test
    void access_token_roundtrip() {
        JwtService svc = newService();
        String token = svc.generateAccess(9L, "alice", List.of("ADMIN"), List.of("rule:edit"));
        Claims claims = svc.parse(token);
        assertEquals("alice", claims.getSubject());
        assertEquals(9L, claims.get("uid", Number.class).longValue());
        assertTrue(svc.isAccess(claims));
        assertEquals(List.of("ADMIN"), svc.claimList(claims, "roles"));
        assertEquals(List.of("rule:edit"), svc.claimList(claims, "perms"));
    }

    @Test
    void refresh_token_type() {
        JwtService svc = newService();
        Claims claims = svc.parse(svc.generateRefresh(1L, "bob"));
        assertFalse(svc.isAccess(claims));
        assertEquals("refresh", claims.get("type"));
    }
}
