-- M3 规则核心：规则、规则版本、规则集、模板、客户覆盖

CREATE TABLE rule (
    id                  BIGINT       NOT NULL PRIMARY KEY,
    rule_key            VARCHAR(128) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    category            VARCHAR(32)  NOT NULL,            -- balance|reconciliation|continuity|cost|integrity|cross_system|regression
    type                VARCHAR(40)  NOT NULL,            -- 5 种机制
    severity            VARCHAR(16)  NOT NULL,            -- INFO|LOW|MEDIUM|HIGH|CRITICAL
    spec_json           TEXT,                             -- 机制参数(sql/left_sql/right_sql/min/max/tolerance/datasourceId 等)
    scope_json          TEXT,                             -- products[]/version_expr/servers[]/rule_sets[]/customers[]
    invariant           TINYINT      NOT NULL DEFAULT 0,
    invariant_metric    VARCHAR(64),
    invariant_tolerance DECIMAL(20,6),
    schedule_json       TEXT,
    alert_json          TEXT,
    current_version     INT          NOT NULL DEFAULT 1,
    enabled             TINYINT      NOT NULL DEFAULT 1,
    source_template_id  BIGINT,
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_rule_key (rule_key, deleted),
    KEY idx_rule_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查规则(当前生效)';

CREATE TABLE rule_version (
    id            BIGINT      NOT NULL PRIMARY KEY,
    rule_id       BIGINT      NOT NULL,
    rule_key      VARCHAR(128) NOT NULL,
    version       INT         NOT NULL,
    severity      VARCHAR(16),
    spec_json     TEXT,
    scope_json    TEXT,
    change_summary VARCHAR(1000),
    author        VARCHAR(64),
    snapshot_json MEDIUMTEXT,                              -- 完整快照，供回滚
    created_at    DATETIME(3),
    UNIQUE KEY uk_rule_version (rule_id, version),
    KEY idx_rv_key (rule_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则历史版本';

CREATE TABLE rule_set (
    id           BIGINT      NOT NULL PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    industry     VARCHAR(32),
    product_line VARCHAR(64),
    version      INT         NOT NULL DEFAULT 1,
    builtin      TINYINT     NOT NULL DEFAULT 0,
    status       VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED', -- PUBLISHED|DRAFT
    remark       VARCHAR(255),
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则集/行业模板';

CREATE TABLE rule_set_item (
    id          BIGINT       NOT NULL PRIMARY KEY,
    rule_set_id BIGINT       NOT NULL,
    rule_key    VARCHAR(128) NOT NULL,
    effective_version_expr VARCHAR(64),
    KEY idx_rsi_set (rule_set_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则集明细';

CREATE TABLE customer_rule (
    id               BIGINT       NOT NULL PRIMARY KEY,
    customer_id      BIGINT       NOT NULL,
    rule_key         VARCHAR(128) NOT NULL,
    override_spec_json TEXT,
    is_local_modified TINYINT     NOT NULL DEFAULT 0,
    enabled          TINYINT      NOT NULL DEFAULT 1,
    effective        TINYINT      NOT NULL DEFAULT 1,
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_customer_rule (customer_id, rule_key, deleted),
    KEY idx_cr_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户规则覆盖/微调';
