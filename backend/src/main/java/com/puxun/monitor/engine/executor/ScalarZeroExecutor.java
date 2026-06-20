package com.puxun.monitor.engine.executor;

import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.engine.CheckContext;
import com.puxun.monitor.engine.model.EngineModels.CheckResultData;
import com.puxun.monitor.engine.model.EngineModels.RuleSpec;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** 标量须约等于 0（如 全局 Σ借−Σ贷=0）。 */
@Component
public class ScalarZeroExecutor implements CheckExecutor {

    @Override public Enums.RuleType type() { return Enums.RuleType.scalar_zero; }

    @Override
    public CheckResultData execute(RuleSpec rule, CheckContext ctx) {
        long t0 = System.currentTimeMillis();
        BigDecimal v = nz(ctx.facade().scalar(rule.datasourceId(), rule.sql(), ctx.baseParams()));
        BigDecimal tol = tol(rule, ctx);
        Map<String, Object> metrics = Map.of("value", v, "tolerance", tol);
        long ms = System.currentTimeMillis() - t0;
        if (v.abs().compareTo(tol) <= 0) {
            return CheckResultData.passed(rule, "标量值 " + v + " 在容差 ±" + tol + " 内", metrics, ms);
        }
        return CheckResultData.failed(rule, "期望约为 0，实际为 " + v + "（容差 ±" + tol + "）", metrics, null, ms);
    }
}
