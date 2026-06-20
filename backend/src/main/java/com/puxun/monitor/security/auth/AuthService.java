package com.puxun.monitor.security.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.security.auth.dto.AuthDtos;
import com.puxun.monitor.security.config.PlatformSecurityProperties;
import com.puxun.monitor.security.domain.SysUser;
import com.puxun.monitor.security.jwt.JwtService;
import com.puxun.monitor.security.mapper.AuthMapper;
import com.puxun.monitor.security.mapper.SysUserMapper;
import com.puxun.monitor.security.support.AuthUser;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PlatformSecurityProperties props;

    public AuthDtos.TokenResponse login(String username, String rawPassword) {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已禁用");
        }
        return issue(user);
    }

    public AuthDtos.TokenResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (Exception e) {
            throw new BizException(ResultCode.UNAUTHORIZED, "refreshToken 无效");
        }
        if (!"refresh".equals(claims.get("type"))) {
            throw new BizException(ResultCode.UNAUTHORIZED, "token 类型错误");
        }
        SysUser user = userMapper.selectById(claims.get("uid", Number.class).longValue());
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户不存在");
        }
        return issue(user);
    }

    private AuthDtos.TokenResponse issue(SysUser user) {
        List<String> roles = authMapper.findRoleCodes(user.getId());
        List<String> perms = authMapper.findPermissions(user.getId());
        String access = jwtService.generateAccess(user.getId(), user.getUsername(), roles, perms);
        String refresh = jwtService.generateRefresh(user.getId(), user.getUsername());
        return new AuthDtos.TokenResponse(access, refresh, props.getJwt().getAccessTtlMinutes());
    }

    public AuthDtos.UserInfo me(AuthUser current) {
        SysUser user = userMapper.selectById(current.getUserId());
        String display = user != null ? user.getDisplayName() : current.getUsername();
        return new AuthDtos.UserInfo(current.getUserId(), current.getUsername(), display,
                current.getRoles(), current.getPermissions());
    }
}
