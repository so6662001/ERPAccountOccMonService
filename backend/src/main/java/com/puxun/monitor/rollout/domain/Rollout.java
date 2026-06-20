package com.puxun.monitor.rollout.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rollout")
public class Rollout extends BaseEntity {

    private String targetType;       // RULE|RULE_SET|TEMPLATE
    private String targetRef;
    private String fromVersion;
    private String toVersion;
    private String dimension;        // BY_SERVER|BY_CUSTOMER|BY_TENANT_PCT
    private String advanceMode;      // MANUAL|AUTO
    private BigDecimal fpThreshold;
    private BigDecimal errorThreshold;
    private String status;           // DRAFT|RUNNING|PAUSED|DONE|ROLLED_BACK
    private Integer currentBatch;
}
