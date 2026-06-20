package com.puxun.monitor.audit;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogMapper mapper;

    /** 异步落库，避免阻塞主流程。 */
    @Async
    public void record(String actor, String role, String action, String target, String changeSummary, String ip) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setRole(role);
        log.setAction(action);
        log.setTarget(target);
        log.setChangeSummary(changeSummary);
        log.setIp(ip);
        log.setCreatedAt(Instant.now());
        mapper.insert(log);
    }

    public IPage<AuditLog> query(String keyword, long page, long size) {
        return mapper.selectPage(new Page<>(page, size),
                Wrappers.<AuditLog>lambdaQuery()
                        .like(keyword != null && !keyword.isBlank(), AuditLog::getActor, keyword)
                        .orderByDesc(AuditLog::getCreatedAt));
    }
}
