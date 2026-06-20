package com.puxun.monitor.baseline;

import com.puxun.monitor.baseline.domain.Baseline;
import com.puxun.monitor.baseline.domain.RegressionFinding;
import com.puxun.monitor.common.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "基线与回归")
@RestController
@RequestMapping("/baselines")
@RequiredArgsConstructor
public class BaselineController {

    private final BaselineService service;

    @Operation(summary = "基线列表")
    @GetMapping
    public ApiResult<List<Baseline>> list(@RequestParam(required = false) Long customerId,
                                          @RequestParam(required = false) String period) {
        return ApiResult.ok(service.list(customerId, period));
    }

    @Operation(summary = "从运行生成基线快照")
    @PostMapping("/snapshot")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH','FINANCE')")
    public ApiResult<Baseline> snapshot(@RequestParam Long runId) {
        return ApiResult.ok(service.snapshot(runId));
    }

    @Operation(summary = "审定基线")
    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    public ApiResult<Baseline> audit(@PathVariable Long id) {
        return ApiResult.ok(service.audit(id));
    }

    @Operation(summary = "重置基线（合法变更后，需审定）")
    @PostMapping("/reset")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    public ApiResult<Baseline> reset(@RequestParam Long runId) {
        return ApiResult.ok(service.reset(runId));
    }

    @Operation(summary = "回归对比（指定基线）")
    @PostMapping("/compare")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH','FINANCE')")
    public ApiResult<List<RegressionFinding>> compare(@RequestParam Long runId, @RequestParam Long baselineId) {
        return ApiResult.ok(service.compare(runId, baselineId));
    }
}
