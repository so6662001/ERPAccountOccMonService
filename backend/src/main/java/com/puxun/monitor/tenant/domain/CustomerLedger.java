package com.puxun.monitor.tenant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_ledger")
public class CustomerLedger extends BaseEntity {
    private Long customerId;
    private String ledgerName;
    private String standard;          // 中国准则|IFRS|税务|自定义
    private Integer monitorEnabled;   // 1/0
}
