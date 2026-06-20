package com.puxun.monitor.rollout.dto;

import com.puxun.monitor.rollout.domain.Rollout;
import com.puxun.monitor.rollout.domain.RolloutBatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public final class RolloutDtos {

    private RolloutDtos() {}

    public record BatchPlan(int seq, String scopeDesc, List<Long> customerIds, Integer observeMinutes) {}

    public record CreateCmd(
            @NotBlank String targetType,
            @NotBlank String targetRef,
            String fromVersion,
            String toVersion,
            String dimension,
            String advanceMode,
            BigDecimal fpThreshold,
            BigDecimal errorThreshold,
            @NotEmpty List<BatchPlan> batches
    ) {}

    public record DetailVO(Rollout rollout, List<RolloutBatch> batches) {}
}
