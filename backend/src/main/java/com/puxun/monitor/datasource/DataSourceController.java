package com.puxun.monitor.datasource;

import com.puxun.monitor.common.ApiResult;
import com.puxun.monitor.datasource.dto.DataSourceDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "数据源")
@RestController
@RequestMapping("/datasources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService service;

    @Operation(summary = "按客户查询数据源")
    @GetMapping
    public ApiResult<List<DataSourceVO>> list(@RequestParam(required = false) Long customerId) {
        return ApiResult.ok(service.listByCustomer(customerId));
    }

    @Operation(summary = "新增/更新数据源")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<DataSourceVO> save(@Valid @RequestBody SaveCmd cmd) {
        return ApiResult.ok(service.save(cmd));
    }

    @Operation(summary = "删除数据源")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResult.ok();
    }

    @Operation(summary = "对已保存数据源探活")
    @PostMapping("/{id}/probe")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<TestResult> probe(@PathVariable Long id) {
        return ApiResult.ok(service.probe(id));
    }

    @Operation(summary = "连通性测试（不保存）")
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<TestResult> test(@Valid @RequestBody TestCmd cmd) {
        return ApiResult.ok(service.test(cmd));
    }

    @Operation(summary = "批量探活")
    @PostMapping("/probe")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<Void> probeAll() {
        service.probeAll();
        return ApiResult.ok();
    }
}
