package com.puxun.monitor.baseline.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("baseline")
public class Baseline extends BaseEntity {

    private Long customerId;
    private String tenantKey;
    private String period;
    private String relatedVersion;
    private String status;        // PENDING | AUDITED
    private String metricsJson;   // {ruleKey: value}
}
