package com.puxun.monitor.datasource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * 数据源相关 DTO。响应一律不含口令。
 */
public final class DataSourceDtos {

    private DataSourceDtos() {}

    public record SaveCmd(
            Long id,
            @NotNull Long customerId,
            String name,
            @NotBlank String dbType,
            @NotBlank String jdbcUrl,
            String username,
            String password,          // 明文，仅入参；为空表示更新时不改口令
            String mode,
            String tenantColumn
    ) {}

    public record TestCmd(
            @NotBlank String dbType,
            @NotBlank String jdbcUrl,
            String username,
            String password
    ) {}

    public record DataSourceVO(
            Long id, Long customerId, String name, String dbType, String jdbcUrl,
            String username, String mode, String tenantColumn,
            String status, Instant lastProbeAt, Integer latencyMs
    ) {}

    public record TestResult(boolean ok, long latencyMs, String message) {}
}
