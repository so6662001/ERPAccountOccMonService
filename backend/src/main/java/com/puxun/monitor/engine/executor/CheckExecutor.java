package com.puxun.monitor.engine.executor;

import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.engine.CheckContext;
import com.puxun.monitor.engine.model.EngineModels.CheckResultData;
import com.puxun.monitor.engine.model.EngineModels.RuleSpec;

import java.math.BigDecimal;

/**
 * 检查机制执行器（策略）。不抛业务异常，异常由引擎统一兜底为 ERROR。
 */
public interface CheckExecutor {

    Enums.RuleType type();

    CheckResultData execute(RuleSpec rule, CheckContext ctx);

    /** 容差比较辅助：NULL 视为 0。 */
    default BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    default BigDecimal tol(RuleSpec rule, CheckContext ctx) {
        return rule.tolerance() != null ? rule.tolerance() : ctx.defaultTolerance();
    }
}
