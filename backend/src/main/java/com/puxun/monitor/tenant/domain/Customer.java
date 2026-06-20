package com.puxun.monitor.tenant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer")
public class Customer extends BaseEntity {

    private String code;
    private String name;
    private String industry;
    private String isolationMode;          // DB_PER_CUSTOMER | TENANT_SHARED
    private String productLine;
    private String deployVersion;          // 当前部署版本(语义化)
    private String serverGroup;
    private String accountingPeriodType;
    private String currentPeriod;
    private String closingRule;
    private String slaLevel;               // GOLD | SILVER | BRONZE
    private String slaAvailability;
    private String slaCheckFreq;
    private String slaAlertSla;
    private String wecomGroupRef;
    private String status;                 // ACTIVE | DISABLED
}
