package com.puxun.monitor.baseline;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puxun.monitor.audit.Audited;
import com.puxun.monitor.baseline.domain.Baseline;
import com.puxun.monitor.baseline.domain.RegressionFinding;
import com.puxun.monitor.baseline.mapper.BaselineMapper;
import com.puxun.monitor.baseline.mapper.RegressionFindingMapper;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.common.enums.Severity;
import com.puxun.monitor.result.domain.CheckResultEntity;
import com.puxun.monitor.result.domain.DetectRun;
import com.puxun.monitor.result.mapper.CheckResultMapper;
import com.puxun.monitor.result.mapper.DetectRunMapper;
import com.puxun.monitor.rule.domain.Rule;
import com.puxun.monitor.rule.mapper.RuleMapper;
import com.puxun.monitor.tenant.domain.Customer;
import com.puxun.monitor.tenant.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * 基线快照、审定、重置与回归对比。仅已审定基线参与回归；回归并入运行结果与门禁。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaselineService {

    private final BaselineMapper baselineMapper;
    private final RegressionFindingMapper findingMapper;
    private final DetectRunMapper runMapper;
    private final CheckResultMapper resultMapper;
    private final RuleMapper ruleMapper;
    private final CustomerMapper customerMapper;
    private final ObjectMapper om;

    private static final BigDecimal DEFAULT_TOL = new BigDecimal("0.005");
    private static final String[] DEFAULT_METRICS = {"value", "left", "diff", "violations"};
    private static final Severity REGRESSION_SEVERITY = Severity.HIGH;

    public List<Baseline> list(Long customerId, String period) {
        return baselineMapper.selectList(Wrappers.<Baseline>lambdaQuery()
                .eq(customerId != null, Baseline::getCustomerId, customerId)
                .eq(period != null && !period.isBlank(), Baseline::getPeriod, period)
                .orderByDesc(Baseline::getCreatedAt));
    }

    /** 从一次运行抽取 invariant 指标生成基线（PENDING）。 */
    @Audited(action = "SNAPSHOT_BASELINE")
    public Baseline snapshot(Long runId) {
        DetectRun run = runMapper.selectById(runId);
        if (run == null) throw new BizException(ResultCode.NOT_FOUND, "运行不存在");
        Customer c = run.getCustomerId() != null ? customerMapper.selectById(run.getCustomerId()) : null;
        Map<String, BigDecimal> metrics = extractInvariantMetrics(runId);

        Baseline b = new Baseline();
        b.setCustomerId(run.getCustomerId());
        b.setPeriod(c != null ? c.getCurrentPeriod() : null);
        b.setRelatedVersion(c != null ? c.getDeployVersion() : null);
        b.setStatus("PENDING");
        b.setMetricsJson(toJson(metrics));
        baselineMapper.insert(b);
        return b;
    }

    @Audited(action = "AUDIT_BASELINE")
    public Baseline audit(Long baselineId) {
        Baseline b = baselineMapper.selectById(baselineId);
        if (b == null) throw new BizException(ResultCode.NOT_FOUND, "基线不存在");
        b.setStatus("AUDITED");
        baselineMapper.updateById(b);
        return b;
    }

    /** 重置基线：以指定运行重新快照并审定，归档同范围旧基线。 */
    @Audited(action = "RESET_BASELINE")
    @Transactional
    public Baseline reset(Long runId) {
        Baseline fresh = snapshot(runId);
        // 归档同 customer+period 的旧 AUDITED 基线
        baselineMapper.update(null, Wrappers.<Baseline>lambdaUpdate()
                .eq(Baseline::getCustomerId, fresh.getCustomerId())
                .eq(Baseline::getPeriod, fresh.getPeriod())
                .eq(Baseline::getStatus, "AUDITED")
                .ne(Baseline::getId, fresh.getId())
                .set(Baseline::getDeleted, 1));
        fresh.setStatus("AUDITED");
        baselineMapper.updateById(fresh);
        return fresh;
    }

    public Baseline currentAudited(Long customerId, String period) {
        return baselineMapper.selectOne(Wrappers.<Baseline>lambdaQuery()
                .eq(Baseline::getCustomerId, customerId)
                .eq(period != null, Baseline::getPeriod, period)
                .eq(Baseline::getStatus, "AUDITED")
                .orderByDesc(Baseline::getCreatedAt).last("LIMIT 1"), false);
    }

    /** 显式对比：指定基线。 */
    public List<RegressionFinding> compare(Long runId, Long baselineId) {
        Baseline b = baselineMapper.selectById(baselineId);
        if (b == null) throw new BizException(ResultCode.NOT_FOUND, "基线不存在");
        if (!"AUDITED".equals(b.getStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "仅已审定基线可参与回归对比");
        }
        return doCompare(runId, b);
    }

    /** 运行后自动回归：取当前已审定基线，对比并把回归并入运行/门禁。无基线则跳过。 */
    @Transactional
    public List<RegressionFinding> autoRegress(DetectRun run) {
        Customer c = customerMapper.selectById(run.getCustomerId());
        if (c == null) return List.of();
        Baseline b = currentAudited(run.getCustomerId(), c.getCurrentPeriod());
        if (b == null) return List.of();
        return doCompare(run.getId(), b);
    }

    private List<RegressionFinding> doCompare(Long runId, Baseline baseline) {
        DetectRun run = runMapper.selectById(runId);
        if (run == null) throw new BizException(ResultCode.NOT_FOUND, "运行不存在");
        Map<String, BigDecimal> baseMetrics = parseMetrics(baseline.getMetricsJson());
        Map<String, BigDecimal> currentMetrics = extractInvariantMetrics(runId);

        List<RegressionFinding> findings = new ArrayList<>();
        int newFailed = 0;
        Severity maxSev = run.getMaxSeverity() != null ? Severity.valueOf(run.getMaxSeverity()) : null;

        for (Map.Entry<String, BigDecimal> e : baseMetrics.entrySet()) {
            String ruleKey = e.getKey();
            BigDecimal baseVal = e.getValue();
            BigDecimal curVal = currentMetrics.get(ruleKey);
            Rule rule = ruleMapper.selectOne(Wrappers.<Rule>lambdaQuery().eq(Rule::getRuleKey, ruleKey));
            BigDecimal tol = (rule != null && rule.getInvariantTolerance() != null) ? rule.getInvariantTolerance() : DEFAULT_TOL;
            String metricName = (rule != null && rule.getInvariantMetric() != null) ? rule.getInvariantMetric() : "value";

            RegressionFinding f = new RegressionFinding();
            f.setRunId(runId);
            f.setBaselineId(baseline.getId());
            f.setRuleKey(ruleKey);
            f.setMetric(metricName);
            f.setBaselineValue(baseVal);
            f.setCurrentValue(curVal);
            f.setTolerance(tol);
            f.setCreatedAt(Instant.now());

            boolean regression;
            if (curVal == null) {
                f.setStatus("REGRESSION");   // 缺当前指标视为异常
                regression = true;
            } else {
                BigDecimal diff = curVal.subtract(baseVal);
                f.setDiff(diff);
                regression = diff.abs().compareTo(tol) > 0;
                f.setStatus(regression ? "REGRESSION" : "OK");
            }
            findingMapper.insert(f);

            if (regression) {
                newFailed++;
                maxSev = Severity.max(maxSev, REGRESSION_SEVERITY);
                insertRegressionResult(runId, ruleKey, rule, baseVal, curVal);
            }
        }

        if (newFailed > 0) {
            run.setFailed(nz(run.getFailed()) + newFailed);
            run.setTotal(nz(run.getTotal()) + newFailed);
            run.setMaxSeverity(maxSev != null ? maxSev.name() : null);
            Severity gate = run.getGateSeverity() != null ? Severity.valueOf(run.getGateSeverity()) : Severity.HIGH;
            run.setGatePassed((maxSev == null || !maxSev.reaches(gate)) ? 1 : 0);
            runMapper.updateById(run);
        }
        return findings;
    }

    private void insertRegressionResult(Long runId, String ruleKey, Rule rule, BigDecimal base, BigDecimal cur) {
        CheckResultEntity e = new CheckResultEntity();
        e.setRunId(runId);
        e.setRuleKey("regression::" + ruleKey);
        e.setRuleName("回归对比：" + (rule != null ? rule.getName() : ruleKey));
        e.setCategory("regression");
        e.setSeverity(REGRESSION_SEVERITY.name());
        e.setStatus("FAILED");
        e.setMessage("检测到回归：基线 " + base + " → 当前 " + cur + "（该指标不应随迭代变化）");
        e.setDurationMs(0L);
        resultMapper.insert(e);
    }

    /** 从运行的通过结果中抽取 invariant 规则指标。 */
    private Map<String, BigDecimal> extractInvariantMetrics(Long runId) {
        List<CheckResultEntity> results = resultMapper.selectList(Wrappers.<CheckResultEntity>lambdaQuery()
                .eq(CheckResultEntity::getRunId, runId).eq(CheckResultEntity::getStatus, "PASSED"));
        Map<String, BigDecimal> metrics = new LinkedHashMap<>();
        for (CheckResultEntity r : results) {
            Rule rule = ruleMapper.selectOne(Wrappers.<Rule>lambdaQuery().eq(Rule::getRuleKey, r.getRuleKey()));
            if (rule == null || rule.getInvariant() == null || rule.getInvariant() != 1) continue;
            BigDecimal v = pickMetric(r.getMetricsJson(), rule.getInvariantMetric());
            if (v != null) metrics.put(r.getRuleKey(), v);
        }
        return metrics;
    }

    private BigDecimal pickMetric(String metricsJson, String preferred) {
        if (metricsJson == null || metricsJson.isBlank()) return null;
        try {
            JsonNode n = om.readTree(metricsJson);
            if (preferred != null && n.hasNonNull(preferred)) return new BigDecimal(n.get(preferred).asText());
            for (String m : DEFAULT_METRICS) {
                if (n.hasNonNull(m)) return new BigDecimal(n.get(m).asText());
            }
        } catch (Exception e) {
            log.debug("解析指标失败: {}", e.getMessage());
        }
        return null;
    }

    private Map<String, BigDecimal> parseMetrics(String json) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return m;
        try {
            JsonNode n = om.readTree(json);
            n.fields().forEachRemaining(en -> m.put(en.getKey(), new BigDecimal(en.getValue().asText())));
        } catch (Exception e) {
            log.warn("解析基线指标失败: {}", e.getMessage());
        }
        return m;
    }

    private String toJson(Object o) {
        try { return om.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }

    private int nz(Integer i) { return i == null ? 0 : i; }
}
