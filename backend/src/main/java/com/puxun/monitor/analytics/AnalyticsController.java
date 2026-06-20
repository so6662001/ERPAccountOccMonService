package com.puxun.monitor.analytics;

import com.puxun.monitor.common.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "趋势与统计")
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService service;

    @Operation(summary = "总览 KPI（客户数/今日通过率/执行量/待处理告警）")
    @GetMapping("/overview")
    public ApiResult<Map<String, Object>> overview() {
        return ApiResult.ok(service.overview());
    }

    @Operation(summary = "通过率趋势")
    @GetMapping("/pass-rate-trend")
    public ApiResult<List<Map<String, Object>>> passRateTrend(@RequestParam(defaultValue = "14") int days) {
        return ApiResult.ok(service.passRateTrend(days));
    }

    @Operation(summary = "失败数按类别（时间序列）")
    @GetMapping("/failures-by-category")
    public ApiResult<List<Map<String, Object>>> failuresByCategory(@RequestParam(defaultValue = "7") int days) {
        return ApiResult.ok(service.failuresByCategory(days));
    }

    @Operation(summary = "Top 高频失败规则")
    @GetMapping("/top-failing-rules")
    public ApiResult<List<Map<String, Object>>> topFailingRules(
            @RequestParam(defaultValue = "14") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResult.ok(service.topFailingRules(days, limit));
    }

    @Operation(summary = "各行业健康度")
    @GetMapping("/industry-health")
    public ApiResult<List<Map<String, Object>>> industryHealth(@RequestParam(defaultValue = "7") int days) {
        return ApiResult.ok(service.industryHealth(days));
    }

    @Operation(summary = "告警平均处置时长(MTTR, 分钟)")
    @GetMapping("/mttr")
    public ApiResult<Double> mttr(@RequestParam(defaultValue = "7") int days) {
        return ApiResult.ok(service.mttrMinutes(days));
    }

    @Operation(summary = "单客户健康度趋势")
    @GetMapping("/customer/{id}/trend")
    public ApiResult<List<Map<String, Object>>> customerTrend(
            @PathVariable Long id, @RequestParam(defaultValue = "14") int days) {
        return ApiResult.ok(service.customerTrend(id, days));
    }
}
