package com.puxun.monitor.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.common.enums.Severity;
import com.puxun.monitor.engine.model.EngineModels.RuleSpec;
import com.puxun.monitor.rule.domain.Rule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleSpecAssemblerTest {

    private final RuleSpecAssembler assembler = new RuleSpecAssembler(new ObjectMapper());

    @Test
    void parse_scalar_equality_rule() {
        Rule r = new Rule();
        r.setRuleKey("reconciliation.ar_control");
        r.setName("应收勾稽");
        r.setCategory("reconciliation");
        r.setType("scalar_equality");
        r.setSeverity("CRITICAL");
        r.setSpecJson("{\"datasourceId\":9,\"leftSql\":\"SELECT SUM(amount) FROM ar_detail\"," +
                "\"rightSql\":\"SELECT closing FROM gl_account_balance\",\"tolerance\":0.005}");
        r.setScopeJson("{\"products\":[\"商贸ERP\"],\"versionExpr\":\">=v3.0\",\"servers\":[]}");
        r.setInvariant(0);

        RuleSpec spec = assembler.assemble(r);
        assertEquals(Enums.RuleType.scalar_equality, spec.type());
        assertEquals(Severity.CRITICAL, spec.severity());
        assertEquals(Enums.RuleCategory.reconciliation, spec.category());
        assertEquals(9L, spec.datasourceId());
        assertEquals("0.005", spec.tolerance().toString());
        assertEquals(">=v3.0", spec.scope().versionExpr());
        assertTrue(spec.scope().products().contains("商贸ERP"));
        assertNotNull(spec.leftSql());
    }

    @Test
    void empty_scope_defaults_to_all() {
        Rule r = new Rule();
        r.setRuleKey("balance.global");
        r.setName("平衡");
        r.setCategory("balance");
        r.setType("scalar_zero");
        r.setSeverity("HIGH");
        r.setSpecJson("{\"datasourceId\":1,\"sql\":\"SELECT 0\"}");
        r.setScopeJson(null);
        r.setInvariant(0);

        RuleSpec spec = assembler.assemble(r);
        assertEquals("*", spec.scope().versionExpr());
        assertTrue(spec.scope().products().isEmpty());
        assertNull(spec.tolerance());
    }
}
