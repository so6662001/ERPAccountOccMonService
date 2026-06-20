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
            outcome.add(safeExecute(rule, ctx));
        }
        return outcome.computeSummary();
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
