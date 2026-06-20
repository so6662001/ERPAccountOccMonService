package com.puxun.monitor.rule.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public final class RuleDtos {

    private RuleDtos() {}

    public record SaveCmd(
            Long id,
            @NotBlank String ruleKey,
            @NotBlank String name,
            @NotBlank String category,
            @NotBlank String type,
            @NotBlank String severity,
            String specJson,
            String scopeJson,
            Integer invariant,
            String invariantMetric,
            BigDecimal invariantTolerance,
            String scheduleJson,
            String alertJson,
            Integer enabled,
            String changeSummary
    ) {}
}
