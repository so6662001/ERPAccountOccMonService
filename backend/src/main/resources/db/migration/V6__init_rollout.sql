-- M6 灰度发布与模板差异同步

CREATE TABLE rollout (
    id           BIGINT       NOT NULL PRIMARY KEY,
    target_type  VARCHAR(20)  NOT NULL,             -- RULE|RULE_SET|TEMPLATE
    target_ref   VARCHAR(128) NOT NULL,             -- ruleKey 或 ruleSetId
    from_version VARCHAR(32),
    to_version   VARCHAR(32),
    dimension    VARCHAR(20)  NOT NULL,             -- BY_SERVER|BY_CUSTOMER|BY_TENANT_PCT
    advance_mode VARCHAR(10)  NOT NULL DEFAULT 'MANUAL',
    fp_threshold     DECIMAL(6,4) NOT NULL DEFAULT 0.0200,
    error_threshold  DECIMAL(6,4) NOT NULL DEFAULT 0.0500,
    status       VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',  -- DRAFT|RUNNING|PAUSED|DONE|ROLLED_BACK
    current_batch INT         NOT NULL DEFAULT 0,
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灰度发布';

CREATE TABLE rollout_batch (
    id            BIGINT      NOT NULL PRIMARY KEY,
    rollout_id    BIGINT      NOT NULL,
    seq           INT         NOT NULL,
    scope_desc    VARCHAR(128),
    scope_json    TEXT,                              -- {customerIds:[...]} / {serverGroup:..}
    observe_minutes INT       NOT NULL DEFAULT 120,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING', -- PENDING|GRAYING|OBSERVING|PASSED|ROLLED_BACK
    metrics_json  TEXT,
    observe_start_at DATETIME(3),
    advanced_by   VARCHAR(64),
    advanced_at   DATETIME(3),
    KEY idx_batch_rollout (rollout_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灰度批次';

CREATE TABLE template_sync (
    id           BIGINT      NOT NULL PRIMARY KEY,
    customer_id  BIGINT      NOT NULL,
    template_id  BIGINT      NOT NULL,
    from_version VARCHAR(32),
    to_version   VARCHAR(32),
    diff_json    MEDIUMTEXT,
    decisions_json TEXT,
    result       VARCHAR(255),
    status       VARCHAR(16) NOT NULL DEFAULT 'DIFFED',   -- DIFFED|APPLIED
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0,
    KEY idx_ts_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板差异同步';
