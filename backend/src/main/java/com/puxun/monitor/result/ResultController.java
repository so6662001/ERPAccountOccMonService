package com.puxun.monitor.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.puxun.monitor.common.ApiResult;
import com.puxun.monitor.common.PageResult;
import com.puxun.monitor.result.domain.DetectRun;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "检测结果")
@RestController
@RequestMapping("/runs")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @Operation(summary = "分页查询检测运行")
    @GetMapping
    public ApiResult<PageResult<DetectRun>> page(
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        IPage<DetectRun> p = resultService.pageRuns(customerId, page, size);
        return ApiResult.ok(PageResult.of(p.getRecords(), p.getTotal(), page, size));
    }

    @Operation(summary = "运行详情（含结果与违规样本）")
    @GetMapping("/{id}")
    public ApiResult<ResultService.RunDetailVO> detail(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        return ApiResult.ok(resultService.detail(id, status, category));
    }
}
