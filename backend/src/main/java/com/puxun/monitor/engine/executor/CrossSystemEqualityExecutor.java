package com.puxun.monitor.engine.executor;

import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.engine.CheckContext;
import com.puxun.monitor.engine.model.EngineModels.CheckResultData;
import com.puxun.monitor.engine.model.EngineModels.RuleSpec;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** 跨两个数据源的标量须相等（如 订单系统应收=财务系统应收）。 */
@Component
public class CrossSystemEqualityExecutor implements CheckExecutor {

    @Override public Enums.RuleType type() { return Enums.RuleType.cross_system_scalar_equality; }

    @Override
    public CheckResultData execute(RuleSpec rule, CheckContext ctx) {
        long t0 = System.currentTimeMillis();
        BigDecimal left = nz(ctx.facade().scalar(rule.leftDatasourceId(), rule.leftSql(), ctx.baseParams()));
        BigDecimal right = nz(ctx.facade().scalar(rule.rightDatasourceId(), rule.rightSql(), ctx.baseParams()));
        BigDecimal diff = left.subtract(right);
        BigDecimal tol = tol(rule, ctx);
        Map<String, Object> metrics = Map.of(
                "left", left, "right", right, "diff", diff, "tolerance", tol,
                "leftDatasourceId", rule.leftDatasourceId(), "rightDatasourceId", rule.rightDatasourceId());
        long ms = System.currentTimeMillis() - t0;
        if (diff.abs().compareTo(tol) <= 0) {
            return CheckResultData.passed(rule, "跨系统一致：" + left + " ≈ " + right, metrics, ms);
        }
        return CheckResultData.failed(rule,
                "跨系统不一致：left=" + left + ", right=" + right + ", 差额=" + diff + "（容差 ±" + tol + "）",
                metrics, null, ms);
    }
}
