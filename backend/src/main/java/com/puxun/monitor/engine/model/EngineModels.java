package com.puxun.monitor.engine.model;

import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.common.enums.Severity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 检查引擎运行期模型集合。
 */
public final class EngineModels {

    private EngineModels() {}

    /** 执行范围。 */
    public record ExecScope(
            Long customerId,
            Enums.IsolationMode mode,
            String tenantColumn,
            List<String> tenantKeys,    // 租户隔离时的租户集合，可空=全部
            String period,
            String productLine,
            String serverGroup,
            String deployVersion
    ) {}

    /** 规则运行期定义（已合并 customer 覆盖）。 */
    public record RuleSpec(
            String ruleKey,
            String name,
            Enums.RuleCategory category,
            Enums.RuleType type,
            Severity severity,
            BigDecimal tolerance,
            Long datasourceId,           // 主数据源
            String sql,                  // scalar_zero / scalar_range / rows_empty
            String leftSql,              // scalar_equality / cross_system
            String rightSql,
            BigDecimal min,
            BigDecimal max,
            Long leftDatasourceId,       // cross_system
            Long rightDatasourceId,
            RuleScope scope,
            boolean invariant,
            String invariantMetric
    ) {}

    /** 单条检查结果。 */
    public record CheckResultData(
            String ruleKey,
            String name,
            Enums.RuleCategory category,
            Severity severity,
            Enums.CheckStatus status,
            String message,
            Map<String, Object> metrics,
            List<Map<String, Object>> samples,
            long durationMs,
            String error
    ) {
        public static CheckResultData passed(RuleSpec r, String msg, Map<String, Object> metrics, long ms) {
            return new CheckResultData(r.ruleKey(), r.name(), r.category(), r.severity(),
                    Enums.CheckStatus.PASSED, msg, metrics, List.of(), ms, null);
        }
        public static CheckResultData failed(RuleSpec r, String msg, Map<String, Object> metrics,
                                             List<Map<String, Object>> samples, long ms) {
            return new CheckResultData(r.ruleKey(), r.name(), r.category(), r.severity(),
                    Enums.CheckStatus.FAILED, msg, metrics, samples == null ? List.of() : samples, ms, null);
        }
        public static CheckResultData error(RuleSpec r, String err, long ms) {
            return new CheckResultData(r.ruleKey(), r.name(), r.category(), r.severity(),
                    Enums.CheckStatus.ERROR, "执行异常: " + err, Map.of(), List.of(), ms, err);
        }
        public static CheckResultData skipped(RuleSpec r, String reason) {
            return new CheckResultData(r.ruleKey(), r.name(), r.category(), r.severity(),
                    Enums.CheckStatus.SKIPPED, reason, Map.of(), List.of(), 0, null);
        }
    }
}
