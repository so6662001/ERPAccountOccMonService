package com.puxun.monitor.engine.executor;

import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.engine.CheckContext;
import com.puxun.monitor.engine.model.EngineModels.CheckResultData;
import com.puxun.monitor.engine.model.EngineModels.RuleSpec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 违规明细查询须返回 0 行（逐凭证不平/孤儿单据/连续性断裂）。失败时抽样违规记录。 */
@Component
public class RowsEmptyExecutor implements CheckExecutor {

    @Override public Enums.RuleType type() { return Enums.RuleType.rows_empty; }

    @Override
    public CheckResultData execute(RuleSpec rule, CheckContext ctx) {
        long t0 = System.currentTimeMillis();
        long violations = ctx.facade().countOf(rule.datasourceId(), rule.sql(), ctx.baseParams());
        Map<String, Object> metrics = Map.of("violations", violations);
        if (violations == 0) {
            return CheckResultData.passed(rule, "无违规记录", metrics, System.currentTimeMillis() - t0);
        }
        List<Map<String, Object>> samples = ctx.facade().rows(rule.datasourceId(), rule.sql(),
                ctx.baseParams(), ctx.sampleLimit());
        return CheckResultData.failed(rule,
                "发现 " + violations + " 条违规记录（展示前 " + samples.size() + " 条）",
                metrics, samples, System.currentTimeMillis() - t0);
    }
}
