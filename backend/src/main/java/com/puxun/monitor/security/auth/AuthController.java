package com.puxun.monitor.security.auth;

import com.puxun.monitor.common.ApiResult;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.security.auth.dto.AuthDtos;
import com.puxun.monitor.security.support.AuthUser;
import com.puxun.monitor.security.support.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public ApiResult<AuthDtos.TokenResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return ApiResult.ok(authService.login(req.username(), req.password()));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public ApiResult<AuthDtos.TokenResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest req) {
        return ApiResult.ok(authService.refresh(req.refreshToken()));
    }

    @Operation(summary = "企业微信扫码回调（占位，待接入自建应用）")
    @GetMapping("/wecom/callback")
    public ApiResult<Void> wecomCallback(@RequestParam(required = false) String code) {
        // TODO: 用 code 换取企业微信用户身份并签发本平台令牌
        throw new BizException(ResultCode.BIZ_ERROR, "企业微信扫码登录待接入");
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public ApiResult<AuthDtos.UserInfo> me() {
        AuthUser u = SecurityUtils.currentUser();
        return ApiResult.ok(authService.me(u));
    }

    @Operation(summary = "当前用户权限点")
    @GetMapping("/me/permissions")
    public ApiResult<List<String>> permissions() {
        AuthUser u = SecurityUtils.currentUser();
        return ApiResult.ok(u == null ? List.of() : u.getPermissions());
    }

    @Operation(summary = "登出（前端清除令牌即可，此处用于审计）")
    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        return ApiResult.ok();
    }
}
