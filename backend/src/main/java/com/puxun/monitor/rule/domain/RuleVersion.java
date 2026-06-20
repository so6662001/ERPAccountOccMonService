package com.puxun.monitor.rule.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
@TableName("rule_version")
public class RuleVersion implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long ruleId;
    private String ruleKey;
    private Integer version;
    private String severity;
    private String specJson;
    private String scopeJson;
    private String changeSummary;
    private String author;
    private String snapshotJson;
    private Instant createdAt;
}
