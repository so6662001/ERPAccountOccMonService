package com.puxun.monitor.rollout;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puxun.monitor.audit.Audited;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.rollout.domain.Rollout;
import com.puxun.monitor.rollout.domain.RolloutBatch;
import com.puxun.monitor.rollout.dto.RolloutDtos.*;
import com.puxun.monitor.rollout.mapper.RolloutBatchMapper;
import com.puxun.monitor.rollout.mapper.RolloutMapper;
import com.puxun.monitor.rule.domain.CustomerRule;
import com.puxun.monitor.rule.domain.Rule;
import com.puxun.monitor.rule.domain.RuleSetItem;
import com.puxun.monitor.rule.mapper.CustomerRuleMapper;
import com.puxun.monitor.rule.mapper.RuleMapper;
import com.puxun.monitor.rule.mapper.RuleSetItemMapper;
import com.puxun.monitor.rule.version.VersionSpec;
import com.puxun.monitor.result.domain.DetectRun;
import com.puxun.monitor.result.mapper.DetectRunMapper;
import com.puxun.monitor.alert.domain.Alert;
import com.puxun.monitor.alert.mapper.AlertMapper;
import com.puxun.monitor.security.support.SecurityUtils;
import com.puxun.monitor.tenant.domain.Customer;
import com.puxun.monitor.tenant.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 灰度发布引擎：分批生效、默认人工确认推进、观察期超阈值自动回滚。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RolloutService {

    private final RolloutMapper rolloutMapper;
    private final RolloutBatchMapper batchMapper;
    private final CustomerMapper customerMapper;
    private final RuleMapper ruleMapper;
    private final RuleSetItemMapper ruleSetItemMapper;
    private final CustomerRuleMapper customerRuleMapper;
    private final DetectRunMapper detectRunMapper;
    private final AlertMapper alertMapper;
    private final ObjectMapper om;

    // ---------------- 生命周期 ----------------

    @Audited(action = "ROLLOUT_CREATE")
    @Transactional
    public Rollout create(CreateCmd cmd) {
        Rollout r = new Rollout();
        r.setTargetType(cmd.targetType());
        r.setTargetRef(cmd.targetRef());
        r.setFromVersion(cmd.fromVersion());
        r.setToVersion(cmd.toVersion());
        r.setDimension(cmd.dimension() != null ? cmd.dimension() : "BY_CUSTOMER");
        r.setAdvanceMode(cmd.advanceMode() != null ? cmd.advanceMode() : "MANUAL");
        r.setFpThreshold(cmd.fpThreshold() != null ? cmd.fpThreshold() : new BigDecimal("0.02"));
        r.setErrorThreshold(cmd.errorThreshold() != null ? cmd.errorThreshold() : new BigDecimal("0.05"));
        r.setStatus("DRAFT");
        r.setCurrentBatch(0);
        rolloutMapper.insert(r);

        for (BatchPlan b : cmd.batches()) {
            RolloutBatch batch = new RolloutBatch();
            batch.setRolloutId(r.getId());
            batch.setSeq(b.seq());
            batch.setScopeDesc(b.scopeDesc());
            batch.setScopeJson(toJson(Map.of("customerIds", b.customerIds() == null ? List.of() : b.customerIds())));
            batch.setObserveMinutes(b.observeMinutes() != null ? b.observeMinutes() : 120);
            batch.setStatus("PENDING");
            batchMapper.insert(batch);
        }
        return r;
    }

    @Audited(action = "ROLLOUT_START")
    @Transactional
    public Rollout start(Long id) {
        Rollout r = mustGet(id);
        if (!"DRAFT".equals(r.getStatus())) throw new BizException(ResultCode.BIZ_ERROR, "仅 DRAFT 可启动");
        List<RolloutBatch> batches = batches(id);
        if (batches.isEmpty()) throw new BizException(ResultCode.BIZ_ERROR, "无批次");
        RolloutBatch first = batches.get(0);
        deploy(r, first);
        r.setStatus("RUNNING");
        r.setCurrentBatch(0);
        rolloutMapper.updateById(r);
        return r;
    }

    /** 人工确认推进到下一批（默认 MANUAL：当前批必须已 PASSED）。 */
    @Audited(action = "ROLLOUT_ADVANCE")
    @Transactional
    public Rollout advance(Long id) {
        Rollout r = mustGet(id);
        if (!"RUNNING".equals(r.getStatus())) throw new BizException(ResultCode.BIZ_ERROR, "灰度未在运行中");
        List<RolloutBatch> batches = batches(id);
        RolloutBatch cur = batches.get(r.getCurrentBatch());
        if (!"PASSED".equals(cur.getStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "当前批次未达标(状态=" + cur.getStatus() + ")，不可推进");
        }
        cur.setAdvancedBy(SecurityUtils.currentUsernameOrSystem());
        cur.setAdvancedAt(Instant.now());
        batchMapper.updateById(cur);

        int next = r.getCurrentBatch() + 1;
        if (next >= batches.size()) {
            r.setStatus("DONE");
        } else {
            deploy(r, batches.get(next));
            r.setCurrentBatch(next);
        }
        rolloutMapper.updateById(r);
        return r;
    }

    @Audited(action = "ROLLOUT_PAUSE")
    public void pause(Long id) {
        Rollout r = mustGet(id);
        r.setStatus("PAUSED");
        rolloutMapper.updateById(r);
    }

    @Audited(action = "ROLLOUT_ROLLBACK")
    @Transactional
    public Rollout rollback(Long id, String reason) {
        Rollout r = mustGet(id);
        // 回滚已部署的全部批次
        List<RolloutBatch> batches = batches(id);
        for (int i = 0; i <= r.getCurrentBatch() && i < batches.size(); i++) {
            revert(r, batches.get(i));
        }
        r.setStatus("ROLLED_BACK");
        rolloutMapper.updateById(r);
        log.warn("灰度回滚 rollout={}, 原因={}", id, reason);
        return r;
    }

    public DetailVO detail(Long id) {
        return new DetailVO(mustGet(id), batches(id));
    }

    public List<Rollout> list() {
        return rolloutMapper.selectList(Wrappers.<Rollout>lambdaQuery().orderByDesc(Rollout::getUpdatedAt));
    }

    // ---------------- 观察评估（定时） ----------------

    @Scheduled(fixedDelay = 60000)
    public void evaluateRunning() {
        List<Rollout> running = rolloutMapper.selectList(Wrappers.<Rollout>lambdaQuery()
                .eq(Rollout::getStatus, "RUNNING"));
        for (Rollout r : running) {
            try {
                evaluateCurrentBatch(r);
            } catch (Exception e) {
                log.warn("评估灰度 {} 失败: {}", r.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void evaluateCurrentBatch(Rollout r) {
        List<RolloutBatch> batches = batches(r.getId());
        if (r.getCurrentBatch() >= batches.size()) return;
        RolloutBatch b = batches.get(r.getCurrentBatch());
        if (!"OBSERVING".equals(b.getStatus())) return;

        Metrics m = collectMetrics(resolveCustomerIds(b), b.getObserveStartAt());
        b.setMetricsJson(toJson(m));

        if (m.errorRate.compareTo(r.getErrorThreshold()) > 0 || m.fpRate.compareTo(r.getFpThreshold()) > 0) {
            revert(r, b);
            b.setStatus("ROLLED_BACK");
            batchMapper.updateById(b);
            r.setStatus("ROLLED_BACK");
            rolloutMapper.updateById(r);
            log.warn("灰度批次超阈值自动回滚 rollout={}, batch={}, errorRate={}", r.getId(), b.getSeq(), m.errorRate);
            return;
        }
        boolean windowElapsed = b.getObserveStartAt() != null &&
                Instant.now().isAfter(b.getObserveStartAt().plus(b.getObserveMinutes(), ChronoUnit.MINUTES));
        if (windowElapsed) {
            b.setStatus("PASSED");
            batchMapper.updateById(b);
            if ("AUTO".equals(r.getAdvanceMode())) {
                advance(r.getId());     // 自动推进
            }
            // MANUAL：保持 PASSED，等待人工 advance
        } else {
            batchMapper.updateById(b);   // 仅更新 metrics
        }
    }

    // ---------------- 部署/回滚（RuleDeployer） ----------------

    private void deploy(Rollout r, RolloutBatch b) {
        List<Long> customerIds = resolveCustomerIds(b);
        List<String> ruleKeys = targetRuleKeys(r);
        for (Long cid : customerIds) {
            Customer c = customerMapper.selectById(cid);
            if (c == null) continue;
            for (String key : ruleKeys) {
                Rule rule = ruleMapper.selectOne(Wrappers.<Rule>lambdaQuery().eq(Rule::getRuleKey, key));
                if (rule == null) continue;
                boolean eff = versionEffective(rule, c.getDeployVersion());   // 仅版本达标纳入
                upsert(cid, key, true, eff);
            }
        }
        b.setStatus("OBSERVING");
        b.setObserveStartAt(Instant.now());
        batchMapper.updateById(b);
    }

    private void revert(Rollout r, RolloutBatch b) {
        List<Long> customerIds = resolveCustomerIds(b);
        List<String> ruleKeys = targetRuleKeys(r);
        for (Long cid : customerIds) {
            for (String key : ruleKeys) {
                CustomerRule cr = customerRuleMapper.selectOne(Wrappers.<CustomerRule>lambdaQuery()
                        .eq(CustomerRule::getCustomerId, cid).eq(CustomerRule::getRuleKey, key));
                if (cr != null) {
                    cr.setEnabled(0);   // 回滚：停用本批生效
                    customerRuleMapper.updateById(cr);
                }
            }
        }
    }

    private void upsert(Long cid, String ruleKey, boolean enabled, boolean effective) {
        CustomerRule cr = customerRuleMapper.selectOne(Wrappers.<CustomerRule>lambdaQuery()
                .eq(CustomerRule::getCustomerId, cid).eq(CustomerRule::getRuleKey, ruleKey));
        if (cr == null) {
            cr = new CustomerRule();
            cr.setCustomerId(cid);
            cr.setRuleKey(ruleKey);
            cr.setIsLocalModified(0);
            cr.setEnabled(enabled ? 1 : 0);
            cr.setEffective(effective ? 1 : 0);
            customerRuleMapper.insert(cr);
        } else {
            cr.setEnabled(enabled ? 1 : 0);
            cr.setEffective(effective ? 1 : 0);
            customerRuleMapper.updateById(cr);
        }
    }

    private List<String> targetRuleKeys(Rollout r) {
        if ("RULE".equals(r.getTargetType())) {
            return List.of(r.getTargetRef());
        }
        // RULE_SET / TEMPLATE
        Long setId = Long.valueOf(r.getTargetRef());
        return ruleSetItemMapper.selectList(Wrappers.<RuleSetItem>lambdaQuery()
                        .eq(RuleSetItem::getRuleSetId, setId))
                .stream().map(RuleSetItem::getRuleKey).toList();
    }

    private boolean versionEffective(Rule rule, String deployVersion) {
        String expr = "*";
        try {
            if (rule.getScopeJson() != null && !rule.getScopeJson().isBlank()) {
                JsonNode n = om.readTree(rule.getScopeJson());
                if (n.hasNonNull("versionExpr")) expr = n.get("versionExpr").asText();
            }
        } catch (Exception ignored) {}
        return VersionSpec.parse(expr).matches(deployVersion);
    }

    private List<Long> resolveCustomerIds(RolloutBatch b) {
        try {
            JsonNode n = om.readTree(b.getScopeJson());
            List<Long> ids = new ArrayList<>();
            if (n.hasNonNull("customerIds") && n.get("customerIds").isArray()) {
                n.get("customerIds").forEach(x -> ids.add(x.asLong()));
            }
            return ids;
        } catch (Exception e) {
            return List.of();
        }
    }

    // ---------------- 指标采集 ----------------

    private record Metrics(int executions, int failures, int errors, BigDecimal errorRate, BigDecimal fpRate) {}

    private Metrics collectMetrics(List<Long> customerIds, Instant since) {
        if (customerIds.isEmpty() || since == null) {
            return new Metrics(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        List<DetectRun> runs = detectRunMapper.selectList(Wrappers.<DetectRun>lambdaQuery()
                .in(DetectRun::getCustomerId, customerIds)
                .ge(DetectRun::getStartedAt, since));
        int exec = 0, fail = 0, err = 0;
        for (DetectRun r : runs) {
            exec += nz(r.getTotal());
            fail += nz(r.getFailed());
            err += nz(r.getErrored());
        }
        BigDecimal errorRate = exec == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(err).divide(BigDecimal.valueOf(exec), 4, RoundingMode.HALF_UP);
        // 误报率 = 人工标记误报数 / 命中失败数（来自告警反馈闭环）
        long fpCount = alertMapper.selectCount(Wrappers.<Alert>lambdaQuery()
                .in(Alert::getCustomerId, customerIds)
                .eq(Alert::getFalsePositive, 1)
                .ge(Alert::getCreatedAt, since));
        BigDecimal fpRate = fail == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(fpCount).divide(BigDecimal.valueOf(fail), 4, RoundingMode.HALF_UP);
        return new Metrics(exec, fail, err, errorRate, fpRate);
    }

    private List<RolloutBatch> batches(Long rolloutId) {
        return batchMapper.selectList(Wrappers.<RolloutBatch>lambdaQuery()
                .eq(RolloutBatch::getRolloutId, rolloutId).orderByAsc(RolloutBatch::getSeq));
    }

    private Rollout mustGet(Long id) {
        Rollout r = rolloutMapper.selectById(id);
        if (r == null) throw new BizException(ResultCode.NOT_FOUND, "灰度发布不存在");
        return r;
    }

    private String toJson(Object o) {
        try { return om.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }

    private int nz(Integer i) { return i == null ? 0 : i; }
}
