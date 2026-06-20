package com.puxun.monitor.rollout.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
@TableName("rollout_batch")
public class RolloutBatch implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long rolloutId;
    private Integer seq;
    private String scopeDesc;
    private String scopeJson;
    private Integer observeMinutes;
    private String status;           // PENDING|GRAYING|OBSERVING|PASSED|ROLLED_BACK
    private String metricsJson;
    private Instant observeStartAt;
    private String advancedBy;
    private Instant advancedAt;
}
