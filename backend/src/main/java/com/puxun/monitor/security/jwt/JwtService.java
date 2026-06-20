package com.puxun.monitor.security.jwt;

import com.puxun.monitor.security.config.PlatformSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JWT 签发与解析。access 携带角色与权限，refresh 仅用于换取新 access。
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final PlatformSecurityProperties props;

    public JwtService(PlatformSecurityProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccess(Long userId, String username, List<String> roles, List<String> perms) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("type", "access")
                .claim("roles", roles)
                .claim("perms", perms)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.getJwt().getAccessTtlMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String generateRefresh(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.getJwt().getRefreshTtlDays(), ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    @SuppressWarnings("unchecked")
    public List<String> claimList(Claims claims, String name) {
        Object v = claims.get(name);
        return v instanceof List ? (List<String>) v : List.of();
    }

    public boolean isAccess(Claims claims) {
        return "access".equals(claims.get("type"));
    }

    public Map<String, Object> peek(Claims claims) {
        return claims;
    }
}
