package com.puxun.monitor.rule;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.puxun.monitor.audit.Audited;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.rule.domain.*;
import com.puxun.monitor.rule.dto.RuleSetDtos.*;
import com.puxun.monitor.rule.mapper.*;
import com.puxun.monitor.rule.version.VersionSpec;
import com.puxun.monitor.tenant.domain.Customer;
import com.puxun.monitor.tenant.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RuleSetService {

    private final RuleSetMapper ruleSetMapper;
    private final RuleSetItemMapper itemMapper;
    private final RuleMapper ruleMapper;
    private final CustomerRuleMapper customerRuleMapper;
    private final CustomerMapper customerMapper;

    public List<RuleSet> list() {
        return ruleSetMapper.selectList(Wrappers.<RuleSet>lambdaQuery().orderByDesc(RuleSet::getUpdatedAt));
    }

    @Audited(action = "EDIT_RULE_SET")
    public RuleSet save(SaveSetCmd cmd) {
        RuleSet s = cmd.id() != null ? ruleSetMapper.selectById(cmd.id()) : new RuleSet();
        if (s == null) throw new BizException(ResultCode.NOT_FOUND, "规则集不存在");
        s.setName(cmd.name());
        s.setIndustry(cmd.industry());
        s.setProductLine(cmd.productLine());
        s.setStatus(cmd.status() != null ? cmd.status() : "PUBLISHED");
        s.setRemark(cmd.remark());
        if (s.getVersion() == null) s.setVersion(1);
        if (s.getBuiltin() == null) s.setBuiltin(0);
        if (cmd.id() != null) ruleSetMapper.updateById(s); else ruleSetMapper.insert(s);
        return s;
    }

    public List<RuleSetItem> items(Long ruleSetId) {
        return itemMapper.selectList(Wrappers.<RuleSetItem>lambdaQuery()
                .eq(RuleSetItem::getRuleSetId, ruleSetId));
    }

    @Audited(action = "EDIT_RULE_SET", target = "addItem")
    public RuleSetItem addItem(ItemCmd cmd) {
        // 校验规则存在
        if (ruleMapper.selectCount(Wrappers.<Rule>lambdaQuery().eq(Rule::getRuleKey, cmd.ruleKey())) == 0) {
            throw new BizException(ResultCode.NOT_FOUND, "规则不存在: " + cmd.ruleKey());
        }
        RuleSetItem item = new RuleSetItem();
        item.setRuleSetId(cmd.ruleSetId());
        item.setRuleKey(cmd.ruleKey());
        item.setEffectiveVersionExpr(cmd.effectiveVersionExpr());
        itemMapper.insert(item);
        return item;
    }

    public void removeItem(Long itemId) {
        itemMapper.deleteById(itemId);
    }

    /**
     * 一键套用：按每条规则的生效版本表达式与目标客户当前部署版本取交集，
     * 仅"版本达标"的规则标记 effective=1，其余 effective=0(保留待升级生效)。
     */
    @Audited(action = "APPLY_TEMPLATE")
    @Transactional
    public ApplyResult apply(ApplyCmd cmd) {
        List<RuleSetItem> items = items(cmd.ruleSetId());
        List<CustomerApplyResult> per = new ArrayList<>();
        for (Long customerId : cmd.customerIds()) {
            Customer c = customerMapper.selectById(customerId);
            if (c == null) continue;
            int effective = 0, skipped = 0;
            for (RuleSetItem item : items) {
                Rule rule = ruleMapper.selectOne(Wrappers.<Rule>lambdaQuery().eq(Rule::getRuleKey, item.getRuleKey()));
                if (rule == null) continue;
                String expr = item.getEffectiveVersionExpr();
                boolean eff = VersionSpec.parse(expr).matches(c.getDeployVersion());
                upsertCustomerRule(customerId, item.getRuleKey(), eff);
                if (eff) effective++; else skipped++;
            }
            per.add(new CustomerApplyResult(customerId, c.getName(), items.size(), effective, skipped));
        }
        return new ApplyResult(per);
    }

    private void upsertCustomerRule(Long customerId, String ruleKey, boolean effective) {
        CustomerRule cr = customerRuleMapper.selectOne(Wrappers.<CustomerRule>lambdaQuery()
                .eq(CustomerRule::getCustomerId, customerId).eq(CustomerRule::getRuleKey, ruleKey));
        if (cr == null) {
            cr = new CustomerRule();
            cr.setCustomerId(customerId);
            cr.setRuleKey(ruleKey);
            cr.setIsLocalModified(0);
            cr.setEnabled(1);
            cr.setEffective(effective ? 1 : 0);
            customerRuleMapper.insert(cr);
        } else {
            // 本地微调过的规则：仅更新生效标记，不覆盖本地 override（冲突由模板差异同步处理）
            cr.setEffective(effective ? 1 : 0);
            if (cr.getIsLocalModified() == null || cr.getIsLocalModified() == 0) {
                cr.setEnabled(1);
            }
            customerRuleMapper.updateById(cr);
        }
    }
}
