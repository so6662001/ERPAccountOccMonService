package com.puxun.monitor.common.enums;

/**
 * 平台共享枚举集中定义（前后端对齐）。
 */
public final class Enums {

    private Enums() {}

    /** 租户隔离模式。 */
    public enum IsolationMode { DB_PER_CUSTOMER, TENANT_SHARED }

    /** 业务库类型。 */
    public enum DbType { MYSQL, ORACLE, SQLSERVER, DM, POSTGRES }

    /** 数据源读写模式。 */
    public enum DsMode { PRIMARY, READ_REPLICA }

    /** 数据源健康状态。 */
    public enum DsStatus { ONLINE, DELAY, FAILED }

    /** 账务约束类别。 */
    public enum RuleCategory { balance, reconciliation, continuity, cost, integrity, cross_system, regression }

    /** 检查机制类型。 */
    public enum RuleType {
        scalar_zero, scalar_equality, scalar_range, rows_empty, cross_system_scalar_equality
    }

    /** 单条检查结果状态。 */
    public enum CheckStatus {
        PASSED, FAILED, ERROR, SKIPPED;
        public boolean isProblem() { return this == FAILED || this == ERROR; }
    }

    /** 任务触发方式。 */
    public enum TriggerType { CRON, EVENT, CI_GATE, MANUAL }

    /** 基线状态。 */
    public enum BaselineStatus { PENDING, AUDITED }

    /** 灰度发布状态。 */
    public enum RolloutStatus { DRAFT, RUNNING, PAUSED, DONE, ROLLED_BACK }

    /** 灰度批次状态。 */
    public enum BatchStatus { PENDING, GRAYING, OBSERVING, PASSED, ROLLED_BACK }

    /** 灰度维度。 */
    public enum RolloutDimension { BY_SERVER, BY_CUSTOMER, BY_TENANT_PCT }

    /** 推进方式。 */
    public enum AdvanceMode { MANUAL, AUTO }

    /** 模板差异类型。 */
    public enum DiffKind { ADDED, MODIFIED, DEPRECATED, LOCAL_CUSTOM }

    /** 模板同步决定。 */
    public enum SyncDecision { KEEP_LOCAL, ADOPT_TEMPLATE, SKIP }

    /** 告警状态。 */
    public enum AlertStatus { PENDING, PROCESSING, CLOSED }

    /** 企业微信渠道类型。 */
    public enum WecomChannelType { WEBHOOK, APP }
}
