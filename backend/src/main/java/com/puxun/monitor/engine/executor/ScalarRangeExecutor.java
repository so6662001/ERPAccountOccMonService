package com.puxun.monitor.engine.executor;

import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.engine.CheckContext;
import com.puxun.monitor.engine.model.EngineModels.CheckResultData;
import com.puxun.monitor.engine.model.EngineModels.RuleSpec;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** 标量须落在 [min,max]（空界忽略；也作不变量指标载体）。 */
@Component
public class ScalarRangeExecutor implements CheckExecutor {

    @Override public Enums.RuleType type() { return Enums.RuleType.scalar_range; }

    @Override
    public CheckResultData execute(RuleSpec rule, CheckContext ctx) {
        long t0 = System.currentTimeMillis();
        BigDecimal v = nz(ctx.facade().scalar(rule.datasourceId(), rule.sql(), ctx.baseParams()));
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("value", v);
        metrics.put("min", rule.min());
        metrics.put("max", rule.max());
        long ms = System.currentTimeMillis() - t0;
        if (rule.min() != null && v.compareTo(rule.min()) < 0) {
            return CheckResultData.failed(rule, "值 " + v + " 低于下限 " + rule.min(), metrics, null, ms);
        }
        if (rule.max() != null && v.compareTo(rule.max()) > 0) {
            return CheckResultData.failed(rule, "值 " + v + " 超过上限 " + rule.max(), metrics, null, ms);
        }
        return CheckResultData.passed(rule, "值 " + v + " 在允许区间内", metrics, ms);
    }
}
