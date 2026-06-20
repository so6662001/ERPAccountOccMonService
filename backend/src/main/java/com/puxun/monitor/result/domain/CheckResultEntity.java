package com.puxun.monitor.result.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("check_result")
public class CheckResultEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long runId;
    private String ruleKey;
    private String ruleName;
    private String category;
    private String severity;
    private String status;
    private String message;
    private String metricsJson;
    private Long durationMs;
    private String error;
}
