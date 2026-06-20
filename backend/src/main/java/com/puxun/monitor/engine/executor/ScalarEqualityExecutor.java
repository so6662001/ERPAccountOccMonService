package com.puxun.monitor.engine.executor;

import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.engine.CheckContext;
import com.puxun.monitor.engine.model.EngineModels.CheckResultData;
import com.puxun.monitor.engine.model.EngineModels.RuleSpec;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** 两段 SQL 标量须相等（如 应收明细=应收总账余额）。 */
@Component
public class ScalarEqualityExecutor implements CheckExecutor {

    @Override public Enums.RuleType type() { return Enums.RuleType.scalar_equality; }

    @Override
    public CheckResultData execute(RuleSpec rule, CheckContext ctx) {
        long t0 = System.currentTimeMillis();
        BigDecimal left = nz(ctx.facade().scalar(rule.datasourceId(), rule.leftSql(), ctx.baseParams()));
        BigDecimal right = nz(ctx.facade().scalar(rule.datasourceId(), rule.rightSql(), ctx.baseParams()));
        BigDecimal diff = left.subtract(right);
        BigDecimal tol = tol(rule, ctx);
        Map<String, Object> metrics = Map.of("left", left, "right", right, "diff", diff, "tolerance", tol);
        long ms = System.currentTimeMillis() - t0;
        if (diff.abs().compareTo(tol) <= 0) {
            return CheckResultData.passed(rule, "两侧相等：" + left + " ≈ " + right, metrics, ms);
        }
        return CheckResultData.failed(rule,
                "两侧不一致：left=" + left + ", right=" + right + ", 差额=" + diff + "（容差 ±" + tol + "）",
                metrics, null, ms);
    }
}
