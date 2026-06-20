package com.puxun.monitor.engine;

import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.engine.executor.CheckExecutor;
import com.puxun.monitor.engine.model.EngineModels.CheckResultData;
import com.puxun.monitor.engine.model.EngineModels.ExecScope;
import com.puxun.monitor.engine.model.EngineModels.RuleSpec;
import com.puxun.monitor.rule.version.VersionGate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检查引擎：版本生效过滤 → 逐条执行（异常隔离）→ 汇总。
 *
 * 说明：当前实现独立库逐规则执行路径；租户隔离的"单 SQL 聚合扫描 + 异常下钻"
 * 见 docs/模块详设-规则引擎.md §5，作为后续性能增强接入（TenantAggregateRewriter）。
 */
@Slf4j
@Service
public class CheckEngine {

    private final Map<Enums.RuleType, CheckExecutor> registry;
    private final BigDecimal defaultTolerance = new BigDecimal("0.005");
    private final int sampleLimit = 20;

    public CheckEngine(List<CheckExecutor> executors) {
        this.registry = executors.stream()
                .collect(Collectors.toMap(CheckExecutor::type, e -> e));
    }

    public RunOutcome run(ExecScope scope, List<RuleSpec> rules,
                          com.puxun.monitor.datasource.runtime.DataSourceFacade facade) {
        RunOutcome outcome = new RunOutcome();
        CheckContext ctx = new CheckContext(facade, scope, defaultTolerance, sampleLimit);
        for (RuleSpec rule : rules) {
            String skip = VersionGate.skipReason(rule.scope(), scope.productLine(), scope.serverGroup(), scope.deployVersion());
            if (skip != null) {
                outcome.add(CheckResultData.skipped(rule, "跳过：" + skip));
                continue;
            }
            // 租户隔离模式下 rows_empty 走"单 SQL 聚合扫描 + 异常租户下钻"
            if (scope.mode() == Enums.IsolationMode.TENANT_SHARED
                    && rule.type() == Enums.RuleType.rows_empty) {
                outcome.add(tenantAggregateScan(rule, ctx));
            } else {
                outcome.add(safeExecute(rule, ctx));
            }
        }
        return outcome.computeSummary();
    }

    /**
     * 租户隔离高效扫描：一条聚合 SQL 跨全租户定位异常租户，避免逐租户 N 次查询。
     * 约定：rows_empty 的违规查询需 SELECT 出租户列（默认 tenant_id）。
     */
    private CheckResultData tenantAggregateScan(RuleSpec rule, CheckContext ctx) {
        long t0 = System.currentTimeMillis();
        String tenantCol = ctx.scope().tenantColumn() != null ? ctx.scope().tenantColumn() : "tenant_id";
        String agg = "SELECT __s." + tenantCol + " AS tenant_key, COUNT(*) AS violations "
                + "FROM (" + rule.sql() + ") __s GROUP BY __s." + tenantCol;
        try {
            List<Map<String, Object>> offending = ctx.facade().rows(
                    rule.datasourceId(), agg, ctx.baseParams(), ctx.sampleLimit());
            long ms = System.currentTimeMillis() - t0;
            if (offending.isEmpty()) {
                return CheckResultData.passed(rule, "全部租户无违规", Map.of("offendingTenants", 0), ms);
            }
            return CheckResultData.failed(rule,
                    "发现 " + offending.size() + " 个异常租户（一次聚合扫描定位，可下钻明细）",
                    Map.of("offendingTenants", offending.size()), offending, ms);
        } catch (Exception e) {
            log.warn("租户聚合扫描异常 ruleKey={}: {}", rule.ruleKey(), e.getMessage());
            return CheckResultData.error(rule, e.getMessage(), System.currentTimeMillis() - t0);
        }
    }

    private CheckResultData safeExecute(RuleSpec rule, CheckContext ctx) {
        CheckExecutor executor = registry.get(rule.type());
        if (executor == null) {
            return CheckResultData.error(rule, "未知检查类型: " + rule.type(), 0);
        }
        long t0 = System.currentTimeMillis();
        try {
            return executor.execute(rule, ctx);
        } catch (Exception e) {
            log.warn("规则执行异常 ruleKey={}: {}", rule.ruleKey(), e.getMessage());
            return CheckResultData.error(rule, e.getMessage(), System.currentTimeMillis() - t0);
        }
    }
}
