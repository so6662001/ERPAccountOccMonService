package com.puxun.monitor.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.puxun.monitor.security.config.PlatformSecurityProperties;
import com.puxun.monitor.security.domain.SysUser;
import com.puxun.monitor.security.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 首次启动若不存在管理员则创建（密码经 BCrypt 加密），并关联 ADMIN 角色。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapAdminRunner implements ApplicationRunner {

    private final PlatformSecurityProperties props;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        var cfg = props.getBootstrapAdmin();
        if (!cfg.isEnabled()) {
            return;
        }
        Long exists = userMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, cfg.getUsername()));
        if (exists != null && exists > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername(cfg.getUsername());
        admin.setPassword(passwordEncoder.encode(cfg.getPassword()));
        admin.setDisplayName("平台管理员");
        admin.setStatus("ACTIVE");
        admin.setCreatedAt(Instant.now());
        admin.setUpdatedAt(Instant.now());
        admin.setCreatedBy("system");
        admin.setUpdatedBy("system");
        admin.setDeleted(0);
        userMapper.insert(admin);

        // 关联 ADMIN 角色（id=1，见 V1 迁移）
        jdbcTemplate.update("INSERT INTO sys_user_role(id, user_id, role_id) VALUES (?,?,1)",
                System.nanoTime(), admin.getId());
        log.info("已创建初始管理员账号: {}（请尽快修改默认密码）", cfg.getUsername());
    }
}
