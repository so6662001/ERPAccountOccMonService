package com.puxun.monitor.rollout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.rollout.TemplateSyncService.ApplyCmd;
import com.puxun.monitor.rollout.TemplateSyncService.DiffResult;
import com.puxun.monitor.rollout.mapper.TemplateSyncMapper;
import com.puxun.monitor.rule.domain.CustomerRule;
import com.puxun.monitor.rule.domain.Rule;
import com.puxun.monitor.rule.domain.RuleSetItem;
import com.puxun.monitor.rule.mapper.CustomerRuleMapper;
import com.puxun.monitor.rule.mapper.RuleMapper;
import com.puxun.monitor.rule.mapper.RuleSetItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TemplateSyncServiceTest {

    private RuleSetItemMapper itemMapper;
    private CustomerRuleMapper customerRuleMapper;
    private RuleMapper ruleMapper;
    private TemplateSyncService service;

    private RuleSetItem item(String key) {
        RuleSetItem i = new RuleSetItem();
        i.setRuleKey(key);
        return i;
    }

    private CustomerRule cr(String key, int localModified) {
        CustomerRule c = new CustomerRule();
        c.setRuleKey(key);
        c.setIsLocalModified(localModified);
        return c;
    }

    @BeforeEach
    void setup() {
        itemMapper = Mockito.mock(RuleSetItemMapper.class);
        customerRuleMapper = Mockito.mock(CustomerRuleMapper.class);
        ruleMapper = Mockito.mock(RuleMapper.class);
        TemplateSyncMapper syncMapper = Mockito.mock(TemplateSyncMapper.class);
        service = new TemplateSyncService(itemMapper, customerRuleMapper, ruleMapper, syncMapper, new ObjectMapper());

        Rule enabled = new Rule();
        enabled.setEnabled(1);
        when(ruleMapper.selectOne(any())).thenReturn(enabled);
        // 模板含 A(新增) / B(非冲突修改) / C(冲突)
        when(itemMapper.selectList(any())).thenReturn(List.of(item("A"), item("B"), item("C")));
        // 本地含 B(未微调) / C(已微调=冲突) / D(本地自定义)
        when(customerRuleMapper.selectList(any()))
                .thenReturn(List.of(cr("B", 0), cr("C", 1), cr("D", 0)));
    }

    @Test
    void diff_classifies_added_modified_conflict_local() {
        DiffResult d = service.diff(1L, 100L);
        assertEquals(Enums.DiffKind.ADDED, find(d, "A").kind());
        assertFalse(find(d, "B").conflict());
        assertTrue(find(d, "C").conflict());
        assertEquals(Enums.DiffKind.LOCAL_CUSTOM, find(d, "D").kind());
    }

    @Test
    void apply_rejects_undecided_conflict() {
        // C 为冲突项但未提供决定 → 拒绝
        ApplyCmd cmd = new ApplyCmd(1L, 100L, Map.of(), true);
        BizException ex = assertThrows(BizException.class, () -> service.apply(cmd));
        assertTrue(ex.getMessage().contains("C"));
    }

    @Test
    void apply_succeeds_when_conflict_decided() {
        ApplyCmd cmd = new ApplyCmd(1L, 100L, Map.of("C", "KEEP_LOCAL"), false);
        assertEquals("APPLIED", service.apply(cmd).getStatus());
    }

    private TemplateSyncService.DiffItem find(DiffResult d, String key) {
        return d.items().stream().filter(i -> i.ruleKey().equals(key)).findFirst().orElseThrow();
    }
}
