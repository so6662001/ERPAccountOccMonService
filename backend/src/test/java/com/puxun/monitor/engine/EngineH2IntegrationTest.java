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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 端到端：在 H2 真实库上跑检查引擎（5 机制 + 租户聚合扫描）。
 * 用 Mockito 让 DataSourceFacade 代理到真实 H2 JdbcTemplate。
 */
class EngineH2IntegrationTest {

    private CheckEngine engine;
    private DataSourceFacade facade;
    private NamedParameterJdbcTemplate npt;

    @BeforeEach
    void setup() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                "jdbc:h2:mem:engine_e2e;DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", "");
        JdbcTemplate jt = new JdbcTemplate(ds);
        jt.execute("DROP TABLE IF EXISTS gl_voucher_entry");
        jt.execute("""
                CREATE TABLE gl_voucher_entry(
                  id INT PRIMARY KEY, voucher_no VARCHAR(32), period VARCHAR(16),
                  account_code VARCHAR(16), debit DECIMAL(20,4), credit DECIMAL(20,4), tenant_id VARCHAR(16))
                """);
        // 租户 t1：平衡；租户 t2：V2 借贷差 200
        jt.update("INSERT INTO gl_voucher_entry VALUES (1,'V1','2026-06','1122',5000,0,'t1')");
        jt.update("INSERT INTO gl_voucher_entry VALUES (2,'V1','2026-06','6001',0,5000,'t1')");
        jt.update("INSERT INTO gl_voucher_entry VALUES (3,'V2','2026-06','1122',5000,0,'t2')");
        jt.update("INSERT INTO gl_voucher_entry VALUES (4,'V2','2026-06','6001',0,4800,'t2')");

        npt = new NamedParameterJdbcTemplate(ds);
        facade = Mockito.mock(DataSourceFacade.class);
        when(facade.scalar(anyLong(), anyString(), anyMap())).thenAnswer(inv ->
                npt.queryForObject(inv.getArgument(1), (Map<String, Object>) inv.getArgument(2), BigDecimal.class));
        when(facade.countOf(anyLong(), anyString(), anyMap())).thenAnswer(inv ->
                npt.queryForObject("SELECT COUNT(*) FROM (" + inv.getArgument(1) + ") t",
                        (Map<String, Object>) inv.getArgument(2), Long.class));
        when(facade.rows(anyLong(), anyString(), anyMap(), anyInt())).thenAnswer(inv ->
                npt.queryForList(inv.getArgument(1), (Map<String, Object>) inv.getArgument(2)));

        engine = new CheckEngine(List.of(new ScalarZeroExecutor(), new ScalarEqualityExecutor(),
                new ScalarRangeExecutor(), new RowsEmptyExecutor(), new CrossSystemEqualityExecutor()));
    }

    private ExecScope dbScope() {
        return new ExecScope(1L, Enums.IsolationMode.DB_PER_CUSTOMER, "tenant_id",
                List.of(), null, null, null, null);
    }

    private ExecScope tenantScope() {
        return new ExecScope(1L, Enums.IsolationMode.TENANT_SHARED, "tenant_id",
                List.of(), null, null, null, null);
    }

    private RuleSpec zero(String sql) {
        return new RuleSpec("balance.global", "全局平衡", Enums.RuleCategory.balance,
                Enums.RuleType.scalar_zero, Severity.CRITICAL, null, 1L, sql,
                null, null, null, null, null, null, RuleScope.empty(), false, null);
    }

    private RuleSpec rowsEmpty(String sql) {
        return new RuleSpec("balance.voucher_each", "逐凭证平衡", Enums.RuleCategory.balance,
                Enums.RuleType.rows_empty, Severity.CRITICAL, null, 1L, sql,
                null, null, null, null, null, null, RuleScope.empty(), false, null);
    }

    @Test
    void scalar_zero_real_sql() {
        var pass = engine.run(dbScope(),
                List.of(zero("SELECT SUM(debit)-SUM(credit) FROM gl_voucher_entry WHERE tenant_id='t1'")), facade);
        assertEquals(1, pass.getPassed());

        var fail = engine.run(dbScope(),
                List.of(zero("SELECT SUM(debit)-SUM(credit) FROM gl_voucher_entry WHERE tenant_id='t2'")), facade);
        assertEquals(1, fail.getFailed());
    }

    @Test
    void rows_empty_real_sql_with_samples() {
        var out = engine.run(dbScope(), List.of(rowsEmpty(
                "SELECT voucher_no FROM gl_voucher_entry GROUP BY voucher_no HAVING ABS(SUM(debit)-SUM(credit))>0.005")), facade);
        assertEquals(1, out.getFailed());
        assertFalse(out.getResults().get(0).samples().isEmpty());
    }

    @Test
    void tenant_aggregate_scan_locates_offending_tenant() {
        // rows_empty SQL 需 select 租户列
        var out = engine.run(tenantScope(), List.of(rowsEmpty(
                "SELECT tenant_id, voucher_no FROM gl_voucher_entry GROUP BY tenant_id, voucher_no " +
                "HAVING ABS(SUM(debit)-SUM(credit))>0.005")), facade);
        assertEquals(1, out.getFailed());
        Map<String, Object> metrics = out.getResults().get(0).metrics();
        assertEquals(1, ((Number) metrics.get("offendingTenants")).intValue()); // 仅 t2 异常
    }
}
