package com.puxun.monitor.alert.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert_channel")
public class AlertChannel extends BaseEntity {

    private String name;
    private String type;             // WEBHOOK | APP
    private String configCipher;     // 加密(webhook url / corp 配置)
    private String msgType;          // MARKDOWN | TEXT | TEMPLATE_CARD
    private String mentionMobiles;   // 逗号分隔
    private String gateSeverity;
    private Integer dedupWindowSec;
    private String quietHoursJson;
    private String escalationJson;
    private String scopeType;        // DEFAULT | CUSTOMER | TENANT_POOL
    private String scopeRef;
    private Integer enabled;
}
