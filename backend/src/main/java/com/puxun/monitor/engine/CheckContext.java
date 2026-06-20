package com.puxun.monitor.engine;

import com.puxun.monitor.datasource.runtime.DataSourceFacade;
import com.puxun.monitor.engine.model.EngineModels.ExecScope;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 检查执行上下文：提供只读数据访问、范围与默认参数。
 */
public record CheckContext(
        DataSourceFacade facade,
        ExecScope scope,
        BigDecimal defaultTolerance,
        int sampleLimit
) {
    /** 公共绑定参数（期间等）。 */
    public Map<String, Object> baseParams() {
        Map<String, Object> p = new HashMap<>();
        if (scope != null && scope.period() != null) {
            p.put("period", scope.period());
        }
        return p;
    }
}
