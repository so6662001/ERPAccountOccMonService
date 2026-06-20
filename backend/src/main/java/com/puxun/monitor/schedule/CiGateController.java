package com.puxun.monitor.schedule;

import com.puxun.monitor.common.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CI 发布门禁接口（供流水线调用，白名单放行）。
 * 返回 exitCode：0 通过、1 阻断发布。
 */
@Tag(name = "CI 门禁")
@RestController
@RequestMapping("/ci-gate")
@RequiredArgsConstructor
public class CiGateController {

    private final CiGateService ciGateService;

    @Operation(summary = "发布前回归/门禁检测")
    @PostMapping("/run")
    public ApiResult<CiGateService.GateResult> run(@Valid @RequestBody CiGateService.GateRequest req) {
        return ApiResult.ok(ciGateService.run(req));
    }
}
