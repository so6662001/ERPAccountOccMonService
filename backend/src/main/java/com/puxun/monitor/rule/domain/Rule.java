package com.puxun.monitor.rule.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rule")
public class Rule extends BaseEntity {

    private String ruleKey;
    private String name;
    private String category;
    private String type;
    private String severity;
    private String specJson;
    private String scopeJson;
    private Integer invariant;
    private String invariantMetric;
    private BigDecimal invariantTolerance;
    private String scheduleJson;
    private String alertJson;
    private Integer currentVersion;
    private Integer enabled;
    private Long sourceTemplateId;
}
