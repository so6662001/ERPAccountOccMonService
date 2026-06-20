package com.puxun.monitor.audit;

import com.puxun.monitor.common.ApiResult;
import com.puxun.monitor.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;

@Tag(name = "审计日志")
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @Operation(summary = "查询审计日志")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH','FINANCE')")
    public ApiResult<PageResult<AuditLog>> query(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        IPage<AuditLog> p = auditService.query(keyword, page, size);
        return ApiResult.ok(PageResult.of(p.getRecords(), p.getTotal(), page, size));
    }
}
