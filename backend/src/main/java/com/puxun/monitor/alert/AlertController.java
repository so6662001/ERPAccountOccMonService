package com.puxun.monitor.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.puxun.monitor.alert.domain.Alert;
import com.puxun.monitor.alert.domain.AlertChannel;
import com.puxun.monitor.alert.wecom.WecomPushService;
import com.puxun.monitor.common.ApiResult;
import com.puxun.monitor.common.PageResult;
import com.puxun.monitor.security.support.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "告警")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final AlertChannelService channelService;

    // ---- 告警中心 ----

    @Operation(summary = "告警分页查询")
    @GetMapping("/alerts")
    public ApiResult<PageResult<Alert>> query(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        IPage<Alert> p = alertService.query(status, severity, customerId, page, size);
        return ApiResult.ok(PageResult.of(p.getRecords(), p.getTotal(), page, size));
    }

    @Operation(summary = "认领告警")
    @PostMapping("/alerts/{id}/claim")
    public ApiResult<Void> claim(@PathVariable Long id) {
        alertService.claim(id, SecurityUtils.currentUsernameOrSystem());
        return ApiResult.ok();
    }

    @Operation(summary = "转交告警")
    @PostMapping("/alerts/{id}/transfer")
    public ApiResult<Void> transfer(@PathVariable Long id, @RequestParam String toUser) {
        alertService.transfer(id, toUser);
        return ApiResult.ok();
    }

    @Operation(summary = "关闭告警")
    @PostMapping("/alerts/{id}/close")
    public ApiResult<Void> close(@PathVariable Long id) {
        alertService.close(id, SecurityUtils.currentUsernameOrSystem());
        return ApiResult.ok();
    }

    @Operation(summary = "标记误报（计入灰度误报率并关闭）")
    @PostMapping("/alerts/{id}/false-positive")
    public ApiResult<Void> falsePositive(@PathVariable Long id) {
        alertService.markFalsePositive(id, SecurityUtils.currentUsernameOrSystem());
        return ApiResult.ok();
    }

    // ---- 移动端（数据与 Web 一致）----

    @Operation(summary = "移动端告警列表")
    @GetMapping("/mobile/alerts")
    public ApiResult<PageResult<Alert>> mobileList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        IPage<Alert> p = alertService.query(status, null, null, page, size);
        return ApiResult.ok(PageResult.of(p.getRecords(), p.getTotal(), page, size));
    }

    // ---- 告警渠道（企业微信）----

    @Operation(summary = "渠道列表")
    @GetMapping("/alert-channels")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<AlertChannel>> channels() {
        return ApiResult.ok(channelService.list());
    }

    @Operation(summary = "新增/更新渠道")
    @PostMapping("/alert-channels")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Long> saveChannel(@RequestBody AlertChannelService.SaveCmd cmd) {
        return ApiResult.ok(channelService.save(cmd));
    }

    @Operation(summary = "渠道测试")
    @PostMapping("/alert-channels/{id}/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<WecomPushService.PushResult> testChannel(@PathVariable Long id) {
        return ApiResult.ok(channelService.test(id));
    }
}
