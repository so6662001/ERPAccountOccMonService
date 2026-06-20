package com.puxun.monitor.rule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class RuleSetDtos {

    private RuleSetDtos() {}

    public record SaveSetCmd(Long id, @NotBlank String name, String industry,
                             String productLine, String status, String remark) {}

    public record ItemCmd(@NotNull Long ruleSetId, @NotBlank String ruleKey, String effectiveVersionExpr) {}

    public record ApplyCmd(@NotNull Long ruleSetId, @NotEmpty List<Long> customerIds) {}

    public record CustomerApplyResult(Long customerId, String customerName,
                                      int total, int effective, int skipped) {}

    public record ApplyResult(List<CustomerApplyResult> perCustomer) {}
}
