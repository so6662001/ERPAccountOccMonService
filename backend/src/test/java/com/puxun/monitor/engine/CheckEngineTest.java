package com.puxun.monitor.engine;

import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.common.enums.Severity;
import com.puxun.monitor.datasource.runtime.DataSourceFacade;
import com.puxun.monitor.engine.executor.*;
import com.puxun.monitor.engine.model.EngineModels.ExecScope;
import com.puxun.monitor.engine.model.EngineModels.RuleSpec;
import com.puxun.monitor.engine.model.RuleScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class CheckEngineTest {

    private DataSourceFacade facade;
    private CheckEngine engine;

    private ExecScope scope() {
        return new ExecScope(1L, Enums.IsolationMode.DB_PER_CUSTOMER, "tenant_id",
                List.of(), "2026-06", "商贸ERP", "华东集群", "v3.8");
    }

    private RuleSpec scalarZero(String sql) {
        return new RuleSpec("balance.global", "全局借贷平衡", Enums.RuleCategory.balance,
                Enums.RuleType.scalar_zero, Severity.CRITICAL, null, 9L, sql,
                null, null, null, null, null, null, RuleScope.empty(), false, null);
    }

    @BeforeEach
    void setup() {
        facade = Mockito.mock(DataSourceFacade.class);
        engine = new CheckEngine(List.of(
                new ScalarZeroExecutor(), new ScalarEqualityExecutor(),
                new ScalarRangeExecutor(), new RowsEmptyExecutor(),
                new CrossSystemEqualityExecutor()));
    }

    @Test
    void scalar_zero_pass_and_fail() {
        when(facade.scalar(eq(9L), anyString(), anyMap())).thenReturn(BigDecimal.ZERO);
        var ok = engine.run(scope(), List.of(scalarZero("SELECT 0")), facade);
        assertEquals(1, ok.getPassed());
        assertTrue(ok.gatePassed(Severity.HIGH));

        when(facade.scalar(eq(9L), anyString(), anyMap())).thenReturn(new BigDecimal("-200"));
        var bad = engine.run(scope(), List.of(scalarZero("SELECT -200")), facade);
        assertEquals(1, bad.getFailed());
        assertEquals(Severity.CRITICAL, bad.getMaxSeverity());
        assertFalse(bad.gatePassed(Severity.HIGH));
    }

    @Test
    void rows_empty_samples_on_violation() {
        var rule = new RuleSpec("balance.voucher_each", "逐凭证平衡", Enums.RuleCategory.balance,
                Enums.RuleType.rows_empty, Severity.CRITICAL, null, 9L, "SELECT * FROM x",
                null, null, null, null, null, null, RuleScope.empty(), false, null);
        when(facade.countOf(eq(9L), anyString(), anyMap())).thenReturn(1L);
        when(facade.rows(eq(9L), anyString(), anyMap(), anyInt()))
                .thenReturn(List.of(Map.of("voucher_no", "V001", "diff", -200)));
        var out = engine.run(scope(), List.of(rule), facade);
        assertEquals(1, out.getFailed());
        assertEquals(1, out.getResults().get(0).samples().size());
    }

    @Test
    void version_gate_skips_when_below() {
        RuleSpec r = new RuleSpec("cost.new", "新成本规则", Enums.RuleCategory.cost,
                Enums.RuleType.scalar_zero, Severity.HIGH, null, 9L, "SELECT 0",
                null, null, null, null, null, null,
                new RuleScope(List.of(), ">=v4.0", List.of(), List.of(), List.of()), false, null);
        var out = engine.run(scope(), List.of(r), facade);  // 当前 v3.8 < v4.0
        assertEquals(1, out.getSkipped());
        assertEquals(0, out.getFailed());
    }

    @Test
    void exception_isolated_as_error() {
        when(facade.scalar(eq(9L), anyString(), anyMap())).thenThrow(new RuntimeException("SQL boom"));
        var out = engine.run(scope(), List.of(scalarZero("SELECT bad")), facade);
        assertEquals(1, out.getErrored());
        assertEquals(Enums.CheckStatus.ERROR, out.getResults().get(0).status());
    }
}
