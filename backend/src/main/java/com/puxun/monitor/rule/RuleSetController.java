package com.puxun.monitor.rule;

import com.puxun.monitor.common.ApiResult;
import com.puxun.monitor.rule.domain.RuleSet;
import com.puxun.monitor.rule.domain.RuleSetItem;
import com.puxun.monitor.rule.dto.RuleSetDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "规则模板库")
@RestController
@RequestMapping("/rule-templates")
@RequiredArgsConstructor
public class RuleSetController {

    private final RuleSetService service;

    @Operation(summary = "规则集/模板列表")
    @GetMapping
    public ApiResult<List<RuleSet>> list() {
        return ApiResult.ok(service.list());
    }

    @Operation(summary = "新增/更新规则集")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<RuleSet> save(@Valid @RequestBody SaveSetCmd cmd) {
        return ApiResult.ok(service.save(cmd));
    }

    @Operation(summary = "规则集明细")
    @GetMapping("/{id}/items")
    public ApiResult<List<RuleSetItem>> items(@PathVariable Long id) {
        return ApiResult.ok(service.items(id));
    }

    @Operation(summary = "添加规则到规则集")
    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<RuleSetItem> addItem(@Valid @RequestBody ItemCmd cmd) {
        return ApiResult.ok(service.addItem(cmd));
    }

    @Operation(summary = "从规则集移除规则")
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<Void> removeItem(@PathVariable Long itemId) {
        service.removeItem(itemId);
        return ApiResult.ok();
    }

    @Operation(summary = "一键套用到客户/租户群（按版本交集下发）")
    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<ApplyResult> apply(@Valid @RequestBody ApplyCmd cmd) {
        return ApiResult.ok(service.apply(cmd));
    }
}
