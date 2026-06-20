-- M5 回归与告警：基线、回归发现、告警、告警渠道、企业微信推送记录

CREATE TABLE baseline (
    id             BIGINT      NOT NULL PRIMARY KEY,
    customer_id    BIGINT      NOT NULL,
    tenant_key     VARCHAR(64),
    period         VARCHAR(16),
    related_version VARCHAR(32),
    status         VARCHAR(16) NOT NULL DEFAULT 'PENDING',   -- PENDING|AUDITED
    metrics_json   TEXT,                                     -- {ruleKey: value}
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0,
    KEY idx_baseline_scope (customer_id, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基线快照';

CREATE TABLE regression_finding (
    id             BIGINT       NOT NULL PRIMARY KEY,
    run_id         BIGINT       NOT NULL,
    baseline_id    BIGINT,
    rule_key       VARCHAR(128),
    metric         VARCHAR(64),
    baseline_value DECIMAL(20,4),
    current_value  DECIMAL(20,4),
    diff           DECIMAL(20,4),
    tolerance      DECIMAL(20,6),
    status         VARCHAR(16),                              -- REGRESSION|OK
    created_at     DATETIME(3),
    KEY idx_rf_run (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回归发现';

CREATE TABLE alert_channel (
    id            BIGINT       NOT NULL PRIMARY KEY,
    name          VARCHAR(128),
    type          VARCHAR(20)  NOT NULL,                     -- WEBHOOK|APP
    config_cipher VARCHAR(1024),                             -- 加密(webhook url / corp 配置)
    msg_type      VARCHAR(20)  NOT NULL DEFAULT 'MARKDOWN',  -- MARKDOWN|TEXT|TEMPLATE_CARD
    mention_mobiles VARCHAR(512),                            -- 逗号分隔
    gate_severity VARCHAR(16)  NOT NULL DEFAULT 'HIGH',
    dedup_window_sec INT       NOT NULL DEFAULT 300,
    quiet_hours_json VARCHAR(255),
    escalation_json  VARCHAR(512),
    scope_type    VARCHAR(20)  NOT NULL DEFAULT 'DEFAULT',   -- DEFAULT|CUSTOMER|TENANT_POOL
    scope_ref     VARCHAR(64),
    enabled       TINYINT      NOT NULL DEFAULT 1,
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警渠道(企业微信)';

CREATE TABLE alert (
    id              BIGINT       NOT NULL PRIMARY KEY,
    run_id          BIGINT,
    check_result_id BIGINT,
    customer_id     BIGINT,
    tenant_key      VARCHAR(64),
    rule_key        VARCHAR(128),
    severity        VARCHAR(16),
    title           VARCHAR(255),
    content_json    TEXT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING', -- PENDING|PROCESSING|CLOSED
    assignee        VARCHAR(64),
    wecom_push_status VARCHAR(16),                           -- SENT|FAILED|SILENCED|MERGED
    closed_at       DATETIME(3),
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0,
    KEY idx_alert_status (status),
    KEY idx_alert_customer (customer_id),
    KEY idx_alert_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警';

CREATE TABLE wecom_push_log (
    id          BIGINT      NOT NULL PRIMARY KEY,
    alert_ids   VARCHAR(512),
    channel_id  BIGINT,
    payload_json TEXT,
    http_status INT,
    result      VARCHAR(255),
    pushed_at   DATETIME(3),
    KEY idx_wpl_channel (channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业微信推送记录';
