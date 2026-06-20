package com.puxun.monitor.tenant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.puxun.monitor.common.ApiResult;
import com.puxun.monitor.common.PageResult;
import com.puxun.monitor.tenant.domain.Customer;
import com.puxun.monitor.tenant.domain.Tenant;
import com.puxun.monitor.tenant.dto.CustomerDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "客户与租户")
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @Operation(summary = "分页查询客户")
    @GetMapping
    public ApiResult<PageResult<Customer>> page(
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String isolationMode,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        IPage<Customer> p = service.page(industry, isolationMode, keyword, page, size);
        return ApiResult.ok(PageResult.of(p.getRecords(), p.getTotal(), page, size));
    }

    @Operation(summary = "新增/更新客户（含账簿/币种/合并口径）")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Long> save(@Valid @RequestBody SaveCmd cmd) {
        return ApiResult.ok(service.save(cmd));
    }

    @Operation(summary = "客户详情（含子资源）")
    @GetMapping("/{id}/detail")
    public ApiResult<CustomerDetailVO> detail(@PathVariable Long id) {
        return ApiResult.ok(service.detail(id));
    }

    @Operation(summary = "客户下的租户")
    @GetMapping("/{id}/tenants")
    public ApiResult<List<Tenant>> tenants(@PathVariable Long id) {
        return ApiResult.ok(service.tenants(id));
    }

    @Operation(summary = "自动发现租户（租户隔离模式）")
    @PostMapping("/{id}/discover-tenants")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<Integer> discover(@PathVariable Long id, @Valid @RequestBody DiscoverCmd cmd) {
        return ApiResult.ok(service.discoverTenants(id, cmd));
    }
}
