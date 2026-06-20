package com.puxun.monitor.tenant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_consolidation")
public class CustomerConsolidation extends BaseEntity {
    private Long customerId;
    private Integer inConsolidation;          // 1/0
    private String consolidationLevel;
    private String consolidationEntity;
    private Long eliminationRuleSetId;        // 可空 = 抵消规则集待沉淀
}
