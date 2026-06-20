package com.puxun.monitor.baseline.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@TableName("regression_finding")
public class RegressionFinding implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long runId;
    private Long baselineId;
    private String ruleKey;
    private String metric;
    private BigDecimal baselineValue;
    private BigDecimal currentValue;
    private BigDecimal diff;
    private BigDecimal tolerance;
    private String status;        // REGRESSION | OK
    private Instant createdAt;
}
