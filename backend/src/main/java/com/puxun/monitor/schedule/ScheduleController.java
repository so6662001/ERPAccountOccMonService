package com.puxun.monitor.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.puxun.monitor.common.ApiResult;
import com.puxun.monitor.common.PageResult;
import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.result.domain.DetectRun;
import com.puxun.monitor.schedule.domain.DetectTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "检测任务")
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class ScheduleController {

    private final TaskService taskService;
    private final DynamicCronScheduler scheduler;

    @Operation(summary = "分页查询任务")
    @GetMapping
    public ApiResult<PageResult<DetectTask>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        IPage<DetectTask> p = taskService.page(page, size);
        return ApiResult.ok(PageResult.of(p.getRecords(), p.getTotal(), page, size));
    }

    @Operation(summary = "新增/更新任务")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<DetectTask> save(@RequestBody DetectTask task) {
        DetectTask saved = taskService.save(task);
        scheduler.refresh();
        return ApiResult.ok(saved);
    }

    @Operation(summary = "立即执行任务")
    @PostMapping("/{id}/run")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<List<DetectRun>> run(@PathVariable Long id) {
        return ApiResult.ok(taskService.runNow(id, Enums.TriggerType.MANUAL));
    }

    @Operation(summary = "暂停任务")
    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<Void> pause(@PathVariable Long id) {
        taskService.setEnabled(id, false);
        scheduler.refresh();
        return ApiResult.ok();
    }

    @Operation(summary = "启用任务")
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<Void> resume(@PathVariable Long id) {
        taskService.setEnabled(id, true);
        scheduler.refresh();
        return ApiResult.ok();
    }
}
