package com.puxun.monitor.tenant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant")
public class Tenant extends BaseEntity {

    private Long customerId;
    private String tenantKey;      // = 业务库 tenant_id 值
    private String name;
    private String currentPeriod;
    private String status;
}
