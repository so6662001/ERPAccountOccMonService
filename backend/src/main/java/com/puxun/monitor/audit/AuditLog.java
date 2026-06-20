package com.puxun.monitor.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 审计日志（只追加，不可改删）。
 */
@Data
@TableName("audit_log")
public class AuditLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String actor;
    private String role;
    private String action;
    private String target;
    private String changeSummary;
    private String ip;
    private Instant createdAt;
}
