package com.puxun.monitor.tenant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_currency")
public class CustomerCurrency extends BaseEntity {
    private Long customerId;
    private String currencyCode;
    private Integer isBase;       // 1=本位币
    private String fxSource;
}
