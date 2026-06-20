package com.puxun.monitor.rule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.puxun.monitor.common.ApiResult;
import com.puxun.monitor.common.PageResult;
import com.puxun.monitor.rule.domain.Rule;
import com.puxun.monitor.rule.domain.RuleVersion;
import com.puxun.monitor.rule.dto.RuleDtos.SaveCmd;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "检查规则")
@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService service;

    @Operation(summary = "分页查询规则")
    @GetMapping
    public ApiResult<PageResult<Rule>> page(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        IPage<Rule> p = service.page(category, severity, keyword, page, size);
        return ApiResult.ok(PageResult.of(p.getRecords(), p.getTotal(), page, size));
    }

    @Operation(summary = "规则详情")
    @GetMapping("/{ruleKey}")
    public ApiResult<Rule> detail(@PathVariable String ruleKey) {
        return ApiResult.ok(service.getByKey(ruleKey));
    }

    @Operation(summary = "新增/更新规则（自动版本化）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<Rule> save(@Valid @RequestBody SaveCmd cmd) {
        return ApiResult.ok(service.save(cmd));
    }

    @Operation(summary = "规则历史版本")
    @GetMapping("/{ruleKey}/versions")
    public ApiResult<List<RuleVersion>> versions(@PathVariable String ruleKey) {
        return ApiResult.ok(service.versions(ruleKey));
    }

    @Operation(summary = "回滚到指定版本")
    @PostMapping("/{ruleKey}/rollback/{version}")
    @PreAuthorize("hasAnyRole('ADMIN','RESEARCH')")
    public ApiResult<Rule> rollback(@PathVariable String ruleKey, @PathVariable int version) {
        return ApiResult.ok(service.rollback(ruleKey, version));
    }
}
