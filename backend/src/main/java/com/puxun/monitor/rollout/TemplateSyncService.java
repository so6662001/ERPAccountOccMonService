package com.puxun.monitor.rollout;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puxun.monitor.audit.Audited;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.rollout.domain.TemplateSync;
import com.puxun.monitor.rollout.mapper.TemplateSyncMapper;
import com.puxun.monitor.rule.domain.CustomerRule;
import com.puxun.monitor.rule.domain.Rule;
import com.puxun.monitor.rule.domain.RuleSetItem;
import com.puxun.monitor.rule.mapper.CustomerRuleMapper;
import com.puxun.monitor.rule.mapper.RuleMapper;
import com.puxun.monitor.rule.mapper.RuleSetItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 模板差异对比与选择性同步：冲突(本地微调过)项必须人工逐条决定，系统不自动覆盖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateSyncService {

    private final RuleSetItemMapper itemMapper;
    private final CustomerRuleMapper customerRuleMapper;
    private final RuleMapper ruleMapper;
    private final TemplateSyncMapper syncMapper;
    private final ObjectMapper om;

    public record DiffItem(String ruleKey, Enums.DiffKind kind, String summary, boolean conflict) {}

    public record DiffResult(Long customerId, Long templateId, List<DiffItem> items) {}

    public record ApplyCmd(Long customerId, Long templateId,
                           Map<String, String> decisions,   // ruleKey -> KEEP_LOCAL|ADOPT_TEMPLATE|SKIP
                           boolean includeAdded) {}

    public DiffResult diff(Long customerId, Long templateId) {
        List<RuleSetItem> templateItems = itemMapper.selectList(
                Wrappers.<RuleSetItem>lambdaQuery().eq(RuleSetItem::getRuleSetId, templateId));
        Set<String> templateKeys = new LinkedHashSet<>();
        templateItems.forEach(i -> templateKeys.add(i.getRuleKey()));

        List<CustomerRule> locals = customerRuleMapper.selectList(
                Wrappers.<CustomerRule>lambdaQuery().eq(CustomerRule::getCustomerId, customerId));
        Map<String, CustomerRule> localMap = new HashMap<>();
        locals.forEach(c -> localMap.put(c.getRuleKey(), c));

        List<DiffItem> items = new ArrayList<>();
        for (String key : templateKeys) {
            Rule rule = ruleMapper.selectOne(Wrappers.<Rule>lambdaQuery().eq(Rule::getRuleKey, key));
            boolean deprecated = rule != null && rule.getEnabled() != null && rule.getEnabled() == 0;
            CustomerRule local = localMap.get(key);
            if (deprecated) {
                items.add(new DiffItem(key, Enums.DiffKind.DEPRECATED, "模板中该规则已弃用", false));
            } else if (local == null) {
                items.add(new DiffItem(key, Enums.DiffKind.ADDED, "模板新增规则", false));
            } else {
                boolean conflict = local.getIsLocalModified() != null && local.getIsLocalModified() == 1;
                items.add(new DiffItem(key, Enums.DiffKind.MODIFIED,
                        conflict ? "本地已微调，与模板更新冲突，需人工决定" : "模板更新(非冲突)", conflict));
            }
        }
        // 本地有、模板没有 → 本地自定义，保留
        for (CustomerRule c : locals) {
            if (!templateKeys.contains(c.getRuleKey())) {
                items.add(new DiffItem(c.getRuleKey(), Enums.DiffKind.LOCAL_CUSTOM, "本地自定义(模板中不存在)，保留", false));
            }
        }
        return new DiffResult(customerId, templateId, items);
    }

    @Audited(action = "TEMPLATE_SYNC")
    @Transactional
    public TemplateSync apply(ApplyCmd cmd) {
        DiffResult diff = diff(cmd.customerId(), cmd.templateId());
        Map<String, String> decisions = cmd.decisions() == null ? Map.of() : cmd.decisions();

        // 冲突项必须人工决定
        for (DiffItem item : diff.items()) {
            if (item.conflict() && !decisions.containsKey(item.ruleKey())) {
                throw new BizException(ResultCode.BIZ_ERROR, "存在未决定的冲突项: " + item.ruleKey());
            }
        }

        int added = 0, adopted = 0, kept = 0, deprecated = 0;
        for (DiffItem item : diff.items()) {
            String decision = decisions.get(item.ruleKey());
            switch (item.kind()) {
                case ADDED -> {
                    if (cmd.includeAdded() && !"SKIP".equals(decision)) {
                        upsertFromTemplate(cmd.customerId(), item.ruleKey());
                        added++;
                    }
                }
                case MODIFIED -> {
                    if ("ADOPT_TEMPLATE".equals(decision)) {
                        adoptTemplate(cmd.customerId(), item.ruleKey());
                        adopted++;
                    } else {
                        kept++;   // KEEP_LOCAL / SKIP / null(非冲突默认不动)
                    }
                }
                case DEPRECATED -> {
                    if ("ADOPT_TEMPLATE".equals(decision)) {
                        disableLocal(cmd.customerId(), item.ruleKey());
                        deprecated++;
                    }
                }
                case LOCAL_CUSTOM -> { /* 永远保留 */ }
            }
        }

        TemplateSync rec = new TemplateSync();
        rec.setCustomerId(cmd.customerId());
        rec.setTemplateId(cmd.templateId());
        rec.setDiffJson(toJson(diff.items()));
        rec.setDecisionsJson(toJson(decisions));
        rec.setResult(String.format("新增%d 采用模板%d 保留本地%d 弃用%d", added, adopted, kept, deprecated));
        rec.setStatus("APPLIED");
        syncMapper.insert(rec);
        return rec;
    }

    private void upsertFromTemplate(Long customerId, String ruleKey) {
        CustomerRule cr = getOrNew(customerId, ruleKey);
        cr.setIsLocalModified(0);
        cr.setEnabled(1);
        cr.setEffective(1);
        save(cr);
    }

    private void adoptTemplate(Long customerId, String ruleKey) {
        CustomerRule cr = getOrNew(customerId, ruleKey);
        cr.setOverrideSpecJson(null);     // 采用模板：清除本地覆盖
        cr.setIsLocalModified(0);
        cr.setEnabled(1);
        save(cr);
    }

    private void disableLocal(Long customerId, String ruleKey) {
        CustomerRule cr = customerRuleMapper.selectOne(Wrappers.<CustomerRule>lambdaQuery()
                .eq(CustomerRule::getCustomerId, customerId).eq(CustomerRule::getRuleKey, ruleKey));
        if (cr != null) { cr.setEnabled(0); customerRuleMapper.updateById(cr); }
    }

    private CustomerRule getOrNew(Long customerId, String ruleKey) {
        CustomerRule cr = customerRuleMapper.selectOne(Wrappers.<CustomerRule>lambdaQuery()
                .eq(CustomerRule::getCustomerId, customerId).eq(CustomerRule::getRuleKey, ruleKey));
        if (cr == null) {
            cr = new CustomerRule();
            cr.setCustomerId(customerId);
            cr.setRuleKey(ruleKey);
        }
        return cr;
    }

    private void save(CustomerRule cr) {
        if (cr.getId() == null) customerRuleMapper.insert(cr); else customerRuleMapper.updateById(cr);
    }

    private String toJson(Object o) {
        try { return om.writeValueAsString(o); } catch (Exception e) { return "[]"; }
    }
}
