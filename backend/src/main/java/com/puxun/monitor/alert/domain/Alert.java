package com.puxun.monitor.alert.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert")
public class Alert extends BaseEntity {

    private Long runId;
    private Long checkResultId;
    private Long customerId;
    private String tenantKey;
    private String ruleKey;
    private String severity;
    private String title;
    private String contentJson;
    private String status;            // PENDING|PROCESSING|CLOSED
    private String assignee;
    private String wecomPushStatus;   // SENT|FAILED|SILENCED|MERGED
    private Integer falsePositive;    // 1=人工标记误报
    private Instant closedAt;
}
