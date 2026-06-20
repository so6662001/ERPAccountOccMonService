package com.puxun.monitor.rule.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_rule")
public class CustomerRule extends BaseEntity {

    private Long customerId;
    private String ruleKey;
    private String overrideSpecJson;
    private Integer isLocalModified;
    private Integer enabled;
    private Integer effective;
}
