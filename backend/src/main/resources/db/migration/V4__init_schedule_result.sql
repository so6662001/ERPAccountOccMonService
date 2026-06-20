-- M4 运行闭环：检测任务、检测运行、检查结果、违规样本

CREATE TABLE detect_task (
    id           BIGINT       NOT NULL PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    trigger_type VARCHAR(20)  NOT NULL,            -- CRON|EVENT|CI_GATE|MANUAL
    cron         VARCHAR(64),
    scope_json   TEXT,                             -- 客户/规则集/类别等筛选
    gate_severity VARCHAR(16) NOT NULL DEFAULT 'HIGH',
    concurrency  INT          NOT NULL DEFAULT 4,
    enabled      TINYINT      NOT NULL DEFAULT 1,
    last_run_at  DATETIME(3),
    next_run_at  DATETIME(3),
    status       VARCHAR(16)  NOT NULL DEFAULT 'IDLE',
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测任务';

CREATE TABLE detect_run (
    id            BIGINT      NOT NULL PRIMARY KEY,
    task_id       BIGINT,
    customer_id   BIGINT,
    tenant_key    VARCHAR(64),
    label         VARCHAR(128),
    trigger_type  VARCHAR(20),
    started_at    DATETIME(3),
    finished_at   DATETIME(3),
    duration_ms   BIGINT,
    total         INT, passed INT, failed INT, errored INT, skipped INT,
    max_severity  VARCHAR(16),
    gate_severity VARCHAR(16),
    gate_passed   TINYINT,
    created_at DATETIME(3),
    KEY idx_run_customer (customer_id),
    KEY idx_run_task (task_id),
    KEY idx_run_started (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测运行';

CREATE TABLE check_result (
    id          BIGINT      NOT NULL PRIMARY KEY,
    run_id      BIGINT      NOT NULL,
    rule_key    VARCHAR(128),
    rule_name   VARCHAR(255),
    category    VARCHAR(32),
    severity    VARCHAR(16),
    status      VARCHAR(16),                       -- PASSED|FAILED|ERROR|SKIPPED
    message     VARCHAR(1000),
    metrics_json TEXT,
    duration_ms BIGINT,
    error       VARCHAR(1000),
    KEY idx_cr_run (run_id),
    KEY idx_cr_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单条检查结果';

CREATE TABLE check_violation_sample (
    id              BIGINT  NOT NULL PRIMARY KEY,
    check_result_id BIGINT  NOT NULL,
    sample_json     TEXT,
    KEY idx_cvs_result (check_result_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规样本';
