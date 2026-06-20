package com.puxun.monitor.rule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puxun.monitor.audit.Audited;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.rule.domain.Rule;
import com.puxun.monitor.rule.domain.RuleVersion;
import com.puxun.monitor.rule.dto.RuleDtos.SaveCmd;
import com.puxun.monitor.rule.mapper.RuleMapper;
import com.puxun.monitor.rule.mapper.RuleVersionMapper;
import com.puxun.monitor.security.support.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleMapper ruleMapper;
    private final RuleVersionMapper versionMapper;
    private final ObjectMapper objectMapper;

    public IPage<Rule> page(String category, String severity, String keyword, long page, long size) {
        return ruleMapper.selectPage(new Page<>(page, size),
                Wrappers.<Rule>lambdaQuery()
                        .eq(category != null && !category.isBlank(), Rule::getCategory, category)
                        .eq(severity != null && !severity.isBlank(), Rule::getSeverity, severity)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(Rule::getName, keyword).or().like(Rule::getRuleKey, keyword))
                        .orderByDesc(Rule::getUpdatedAt));
    }

    public Rule getByKey(String ruleKey) {
        Rule r = ruleMapper.selectOne(Wrappers.<Rule>lambdaQuery().eq(Rule::getRuleKey, ruleKey));
        if (r == null) throw new BizException(ResultCode.NOT_FOUND, "规则不存在: " + ruleKey);
        return r;
    }

    @Audited(action = "EDIT_RULE")
    @Transactional
    public Rule save(SaveCmd cmd) {
        Rule existing = ruleMapper.selectOne(Wrappers.<Rule>lambdaQuery().eq(Rule::getRuleKey, cmd.ruleKey()));
        Rule r = existing != null ? existing : new Rule();
        int newVersion = existing != null ? (nz(existing.getCurrentVersion()) + 1) : 1;

        r.setRuleKey(cmd.ruleKey());
        r.setName(cmd.name());
        r.setCategory(cmd.category());
        r.setType(cmd.type());
        r.setSeverity(cmd.severity());
        r.setSpecJson(cmd.specJson());
        r.setScopeJson(cmd.scopeJson());
        r.setInvariant(cmd.invariant() != null ? cmd.invariant() : 0);
        r.setInvariantMetric(cmd.invariantMetric());
        r.setInvariantTolerance(cmd.invariantTolerance());
        r.setScheduleJson(cmd.scheduleJson());
        r.setAlertJson(cmd.alertJson());
        r.setEnabled(cmd.enabled() != null ? cmd.enabled() : 1);
        r.setCurrentVersion(newVersion);

        if (existing != null) ruleMapper.updateById(r); else ruleMapper.insert(r);
        writeVersion(r, newVersion, cmd.changeSummary());
        return r;
    }

    /** 回滚到指定历史版本：以旧快照生成新版本（不物理回退）。 */
    @Audited(action = "EDIT_RULE", target = "rollback")
    @Transactional
    public Rule rollback(String ruleKey, int targetVersion) {
        Rule r = getByKey(ruleKey);
        RuleVersion snap = versionMapper.selectOne(Wrappers.<RuleVersion>lambdaQuery()
                .eq(RuleVersion::getRuleId, r.getId()).eq(RuleVersion::getVersion, targetVersion));
        if (snap == null) throw new BizException(ResultCode.NOT_FOUND, "版本不存在: v" + targetVersion);

        r.setSpecJson(snap.getSpecJson());
        r.setScopeJson(snap.getScopeJson());
        r.setSeverity(snap.getSeverity());
        int newVersion = nz(r.getCurrentVersion()) + 1;
        r.setCurrentVersion(newVersion);
        ruleMapper.updateById(r);
        writeVersion(r, newVersion, "回滚到 v" + targetVersion);
        return r;
    }

    public List<RuleVersion> versions(String ruleKey) {
        Rule r = getByKey(ruleKey);
        return versionMapper.selectList(Wrappers.<RuleVersion>lambdaQuery()
                .eq(RuleVersion::getRuleId, r.getId())
                .orderByDesc(RuleVersion::getVersion));
    }

    private void writeVersion(Rule r, int version, String changeSummary) {
        RuleVersion v = new RuleVersion();
        v.setRuleId(r.getId());
        v.setRuleKey(r.getRuleKey());
        v.setVersion(version);
        v.setSeverity(r.getSeverity());
        v.setSpecJson(r.getSpecJson());
        v.setScopeJson(r.getScopeJson());
        v.setChangeSummary(changeSummary);
        v.setAuthor(SecurityUtils.currentUsernameOrSystem());
        v.setSnapshotJson(snapshot(r));
        v.setCreatedAt(Instant.now());
        versionMapper.insert(v);
    }

    private String snapshot(Rule r) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "ruleKey", r.getRuleKey(),
                    "name", r.getName() == null ? "" : r.getName(),
                    "category", r.getCategory() == null ? "" : r.getCategory(),
                    "type", r.getType() == null ? "" : r.getType(),
                    "severity", r.getSeverity() == null ? "" : r.getSeverity(),
                    "specJson", r.getSpecJson() == null ? "" : r.getSpecJson(),
                    "scopeJson", r.getScopeJson() == null ? "" : r.getScopeJson()
            ));
        } catch (Exception e) {
            return "{}";
        }
    }

    private int nz(Integer i) {
        return i == null ? 0 : i;
    }
}
