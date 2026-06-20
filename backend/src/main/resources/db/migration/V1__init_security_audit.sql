-- 普讯科技产品应用数据监测平台 — 平台库 M1：安全与审计
-- 平台库与业务库严格隔离；本库仅存平台自身数据。

CREATE TABLE sys_user (
    id           BIGINT       NOT NULL PRIMARY KEY,
    username     VARCHAR(64)  NOT NULL,
    password     VARCHAR(100) NOT NULL,
    display_name VARCHAR(64),
    mobile       VARCHAR(32),
    wecom_user_id VARCHAR(64),
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME(3),
    updated_at   DATETIME(3),
    created_by   VARCHAR(64),
    updated_by   VARCHAR(64),
    deleted      TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_username (username, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台用户';

CREATE TABLE sys_role (
    id         BIGINT      NOT NULL PRIMARY KEY,
    code       VARCHAR(32) NOT NULL,
    name       VARCHAR(64) NOT NULL,
    remark     VARCHAR(255),
    created_at DATETIME(3),
    updated_at DATETIME(3),
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    deleted    TINYINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_code (code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE sys_user_role (
    id      BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_ur_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色';

CREATE TABLE role_permission (
    id         BIGINT      NOT NULL PRIMARY KEY,
    role_id    BIGINT      NOT NULL,
    permission VARCHAR(64) NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, permission),
    KEY idx_rp_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限点';

CREATE TABLE audit_log (
    id             BIGINT       NOT NULL PRIMARY KEY,
    actor          VARCHAR(64),
    role           VARCHAR(128),
    action         VARCHAR(64),
    target         VARCHAR(255),
    change_summary VARCHAR(1000),
    ip             VARCHAR(64),
    created_at     DATETIME(3),
    KEY idx_audit_action (action),
    KEY idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志(只追加)';

-- 内置角色
INSERT INTO sys_role (id, code, name, remark, created_at, updated_at, created_by, deleted) VALUES
 (1, 'ADMIN',    '平台管理员', '全部权限',     NOW(3), NOW(3), 'system', 0),
 (2, 'RESEARCH', '研发',       '规则/模板/灰度', NOW(3), NOW(3), 'system', 0),
 (3, 'FINANCE',  '财务',       '基线审定/重置', NOW(3), NOW(3), 'system', 0),
 (4, 'READONLY', '只读',       '只读查看',     NOW(3), NOW(3), 'system', 0);

-- 角色-权限点（粗粒度，可按需细化）
INSERT INTO role_permission (id, role_id, permission) VALUES
 (1, 1, 'rule:edit'), (2, 1, 'rule:apply_template'), (3, 1, 'rollout:exec'),
 (4, 1, 'baseline:reset'), (5, 1, 'datasource:edit'), (6, 1, 'customer:edit'),
 (7, 2, 'rule:edit'), (8, 2, 'rule:apply_template'), (9, 2, 'rollout:exec'),
 (10, 3, 'baseline:reset');
