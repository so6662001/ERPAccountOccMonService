package com.puxun.monitor.datasource.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_source")
public class DataSourceEntity extends BaseEntity {

    private Long customerId;
    private String name;
    private String dbType;          // Enums.DbType
    private String jdbcUrl;
    private String username;
    private String passwordCipher;  // 加密存储
    private String mode;            // PRIMARY | READ_REPLICA
    private String tenantColumn;    // 默认 tenant_id
    private String status;          // ONLINE | DELAY | FAILED
    private Instant lastProbeAt;
    private Integer latencyMs;
}
