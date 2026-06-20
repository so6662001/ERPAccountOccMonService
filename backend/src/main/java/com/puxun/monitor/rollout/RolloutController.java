package com.puxun.monitor.rollout;

import com.puxun.monitor.common.ApiResult;
import com.puxun.monitor.rollout.domain.Rollout;
import com.puxun.monitor.rollout.domain.TemplateSync;
import com.puxun.monitor.rollout.dto.RolloutDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "灰度发布与模板同步")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class RolloutController {

    private final RolloutService rolloutService;
    private final TemplateSyncService templateSyncService;

    // ---- 灰度发布 ----

    @Operation(summary = "灰度列表")
    @GetMapping("/rollouts")
    public ApiResult<List<Rollout>> list() {
        return ApiResult.ok(rolloutService.list());
    }

    @Operation(summary = "新建灰度发布")
    @PostMapping("/rollouts")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<Rollout> create(@Valid @RequestBody CreateCmd cmd) {
        return ApiResult.ok(rolloutService.create(cmd));
    }

    @Operation(summary = "启动灰度（部署第 1 批）")
    @PostMapping("/rollouts/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<Rollout> start(@PathVariable Long id) {
        return ApiResult.ok(rolloutService.start(id));
    }

    @Operation(summary = "人工确认推进下一批")
    @PostMapping("/rollouts/{id}/advance")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<Rollout> advance(@PathVariable Long id) {
        return ApiResult.ok(rolloutService.advance(id));
    }

    @Operation(summary = "暂停灰度")
    @PostMapping("/rollouts/{id}/pause")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<Void> pause(@PathVariable Long id) {
        rolloutService.pause(id);
        return ApiResult.ok();
    }

    @Operation(summary = "回滚灰度")
    @PostMapping("/rollouts/{id}/rollback")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<Rollout> rollback(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return ApiResult.ok(rolloutService.rollback(id, reason != null ? reason : "人工回滚"));
    }

    @Operation(summary = "灰度详情(含批次进度)")
    @GetMapping("/rollouts/{id}")
    public ApiResult<DetailVO> detail(@PathVariable Long id) {
        return ApiResult.ok(rolloutService.detail(id));
    }

    // ---- 模板差异同步 ----

    @Operation(summary = "模板差异对比")
    @GetMapping("/template-sync/diff")
    public ApiResult<TemplateSyncService.DiffResult> diff(@RequestParam Long customerId, @RequestParam Long templateId) {
        return ApiResult.ok(templateSyncService.diff(customerId, templateId));
    }

    @Operation(summary = "选择性同步(冲突项需人工决定)")
    @PostMapping("/template-sync/apply")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<TemplateSync> apply(@RequestBody TemplateSyncService.ApplyCmd cmd) {
        return ApiResult.ok(templateSyncService.apply(cmd));
    }
}
