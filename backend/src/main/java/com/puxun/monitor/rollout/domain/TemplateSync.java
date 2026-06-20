package com.puxun.monitor.rollout.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("template_sync")
public class TemplateSync extends BaseEntity {

    private Long customerId;
    private Long templateId;
    private String fromVersion;
    private String toVersion;
    private String diffJson;
    private String decisionsJson;
    private String result;
    private String status;           // DIFFED|APPLIED
}
