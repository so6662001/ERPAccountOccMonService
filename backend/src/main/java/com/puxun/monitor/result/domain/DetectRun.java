package com.puxun.monitor.result.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
@TableName("detect_run")
public class DetectRun implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long taskId;
    private Long customerId;
    private String tenantKey;
    private String label;
    private String triggerType;
    private Instant startedAt;
    private Instant finishedAt;
    private Long durationMs;
    private Integer total;
    private Integer passed;
    private Integer failed;
    private Integer errored;
    private Integer skipped;
    private String maxSeverity;
    private String gateSeverity;
    private Integer gatePassed;
    private Instant createdAt;
}
