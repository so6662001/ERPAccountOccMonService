package com.puxun.monitor.rule.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rule_set")
public class RuleSet extends BaseEntity {

    private String name;
    private String industry;
    private String productLine;
    private Integer version;
    private Integer builtin;
    private String status;    // PUBLISHED | DRAFT
    private String remark;
}
