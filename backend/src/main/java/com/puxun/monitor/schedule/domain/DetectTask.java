package com.puxun.monitor.schedule.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.puxun.monitor.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("detect_task")
public class DetectTask extends BaseEntity {

    private String name;
    private String triggerType;     // CRON|EVENT|CI_GATE|MANUAL
    private String cron;
    private String scopeJson;       // {"customerIds":[...]} 或 {"industry":..,"isolationMode":..}；空=全部ACTIVE
    private String gateSeverity;    // INFO..CRITICAL
    private Integer concurrency;
    private Integer enabled;
    private Instant lastRunAt;
    private Instant nextRunAt;
    private String status;          // IDLE|RUNNING|PAUSED
}
