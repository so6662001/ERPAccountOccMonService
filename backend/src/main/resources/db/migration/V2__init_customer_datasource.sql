-- M2 数据底座：客户/租户/账簿/币种/合并口径/数据源

CREATE TABLE customer (
    id                    BIGINT       NOT NULL PRIMARY KEY,
    code                  VARCHAR(64)  NOT NULL,
    name                  VARCHAR(128) NOT NULL,
    industry              VARCHAR(32),
    isolation_mode        VARCHAR(20)  NOT NULL,             -- DB_PER_CUSTOMER | TENANT_SHARED
    product_line          VARCHAR(64),
    deploy_version        VARCHAR(32),                       -- 当前部署版本(语义化)
    server_group          VARCHAR(64),
    accounting_period_type VARCHAR(20),                      -- 自然月|445|项目期间制|自定义
    current_period        VARCHAR(16),                       -- 如 2026-06
    closing_rule          VARCHAR(255),
    sla_level             VARCHAR(16),                       -- GOLD|SILVER|BRONZE
    sla_availability      VARCHAR(16),
    sla_check_freq        VARCHAR(32),
    sla_alert_sla         VARCHAR(64),
    wecom_group_ref       VARCHAR(128),
    status                VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_customer_code (code, deleted),
    KEY idx_customer_industry (industry)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户/租户群';

CREATE TABLE tenant (
    id            BIGINT      NOT NULL PRIMARY KEY,
    customer_id   BIGINT      NOT NULL,
    tenant_key    VARCHAR(64) NOT NULL,                      -- = 业务库 tenant_id 值
    name          VARCHAR(128),
    current_period VARCHAR(16),
    status        VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tenant (customer_id, tenant_key, deleted),
    KEY idx_tenant_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户(租户隔离模式)';

CREATE TABLE customer_ledger (
    id           BIGINT      NOT NULL PRIMARY KEY,
    customer_id  BIGINT      NOT NULL,
    ledger_name  VARCHAR(64) NOT NULL,
    standard     VARCHAR(32),                                -- 中国准则|IFRS|税务|自定义
    monitor_enabled TINYINT  NOT NULL DEFAULT 1,
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0,
    KEY idx_ledger_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多账簿';

CREATE TABLE customer_currency (
    id          BIGINT      NOT NULL PRIMARY KEY,
    customer_id BIGINT      NOT NULL,
    currency_code VARCHAR(8) NOT NULL,
    is_base     TINYINT     NOT NULL DEFAULT 0,
    fx_source   VARCHAR(64),
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0,
    KEY idx_currency_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多币种';

CREATE TABLE customer_consolidation (
    id                   BIGINT  NOT NULL PRIMARY KEY,
    customer_id          BIGINT  NOT NULL,
    in_consolidation     TINYINT NOT NULL DEFAULT 0,
    consolidation_level  VARCHAR(64),
    consolidation_entity VARCHAR(128),
    elimination_rule_set_id BIGINT,                          -- 可空 = 抵消规则集待沉淀
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_consol_customer (customer_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合并报表口径';

CREATE TABLE data_source (
    id              BIGINT       NOT NULL PRIMARY KEY,
    customer_id     BIGINT       NOT NULL,
    name            VARCHAR(128),
    db_type         VARCHAR(20)  NOT NULL,                   -- MYSQL|ORACLE|SQLSERVER|DM|POSTGRES
    jdbc_url        VARCHAR(512) NOT NULL,
    username        VARCHAR(128),
    password_cipher VARCHAR(512),                            -- 加密存储
    mode            VARCHAR(20)  NOT NULL DEFAULT 'READ_REPLICA', -- PRIMARY|READ_REPLICA
    tenant_column   VARCHAR(64)  DEFAULT 'tenant_id',
    status          VARCHAR(16)  NOT NULL DEFAULT 'ONLINE',  -- ONLINE|DELAY|FAILED
    last_probe_at   DATETIME(3),
    latency_ms      INT,
    created_at DATETIME(3), updated_at DATETIME(3), created_by VARCHAR(64), updated_by VARCHAR(64),
    deleted    TINYINT NOT NULL DEFAULT 0,
    KEY idx_ds_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务库数据源(只读)';
