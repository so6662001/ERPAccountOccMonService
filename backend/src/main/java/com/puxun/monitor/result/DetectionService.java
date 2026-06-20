package com.puxun.monitor.result;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.common.enums.Severity;
import com.puxun.monitor.datasource.runtime.DataSourceFacade;
import com.puxun.monitor.engine.CheckEngine;
import com.puxun.monitor.engine.RuleSpecAssembler;
import com.puxun.monitor.engine.RunOutcome;
import com.puxun.monitor.engine.model.EngineModels.ExecScope;
import com.puxun.monitor.engine.model.EngineModels.RuleSpec;
import com.puxun.monitor.engine.model.EngineModels.CheckResultData;
import com.puxun.monitor.rule.domain.CustomerRule;
import com.puxun.monitor.rule.domain.Rule;
import com.puxun.monitor.rule.mapper.CustomerRuleMapper;
import com.puxun.monitor.rule.mapper.RuleMapper;
import com.puxun.monitor.result.domain.CheckResultEntity;
import com.puxun.monitor.result.domain.CheckViolationSample;
import com.puxun.monitor.result.domain.DetectRun;
import com.puxun.monitor.result.mapper.CheckResultMapper;
import com.puxun.monitor.result.mapper.CheckViolationSampleMapper;
import com.puxun.monitor.result.mapper.DetectRunMapper;
import com.puxun.monitor.tenant.domain.Customer;
import com.puxun.monitor.tenant.mapper.CustomerMapper;
import com.puxun.monitor.baseline.BaselineService;
import com.puxun.monitor.alert.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 检测执行：构建范围 → 解析生效规则 → 引擎执行 → 落库 → 返回运行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionService {

    private final CustomerMapper customerMapper;
    private final CustomerRuleMapper customerRuleMapper;
    private final RuleMapper ruleMapper;
    private final RuleSpecAssembler assembler;
    private final CheckEngine engine;
    private final DataSourceFacade facade;
    private final DetectRunMapper runMapper;
    private final CheckResultMapper resultMapper;
    private final CheckViolationSampleMapper sampleMapper;
    private final BaselineService baselineService;
    private final AlertService alertService;
    private final ObjectMapper om;

    @Transactional
    public DetectRun runForCustomer(Long customerId, Long taskId, String label,
                                    Enums.TriggerType trigger, Severity gate) {
        Customer c = customerMapper.selectById(customerId);
        if (c == null) throw new BizException(ResultCode.NOT_FOUND, "客户不存在");

        ExecScope scope = new ExecScope(
                customerId,
                Enums.IsolationMode.valueOf(c.getIsolationMode()),
                "tenant_id",
                List.of(),
                c.getCurrentPeriod(),
                c.getProductLine(),
                c.getServerGroup(),
                c.getDeployVersion()
        );

        List<RuleSpec> specs = resolveEffectiveRules(customerId);
        Instant start = Instant.now();
        RunOutcome outcome = engine.run(scope, specs, facade);
        Instant end = Instant.now();

        DetectRun run = persist(c, taskId, label, trigger, gate, outcome, start, end);

        // 迭代回归：与已审定基线对比，回归并入运行与门禁（无基线则跳过）
        try {
            baselineService.autoRegress(run);
        } catch (Exception e) {
            log.warn("回归对比失败 run={}: {}", run.getId(), e.getMessage());
        }
        // 失败/回归聚合为告警并按策略推送企业微信
        try {
            alertService.raise(run);
        } catch (Exception e) {
            log.warn("告警触发失败 run={}: {}", run.getId(), e.getMessage());
        }
        return run;
    }

    /** 客户生效规则：customer_rule(enabled & effective) 关联 rule，组装为 RuleSpec。 */
    private List<RuleSpec> resolveEffectiveRules(Long customerId) {
        List<CustomerRule> bindings = customerRuleMapper.selectList(Wrappers.<CustomerRule>lambdaQuery()
                .eq(CustomerRule::getCustomerId, customerId)
                .eq(CustomerRule::getEnabled, 1)
                .eq(CustomerRule::getEffective, 1));
        return bindings.stream()
                .map(b -> ruleMapper.selectOne(Wrappers.<Rule>lambdaQuery().eq(Rule::getRuleKey, b.getRuleKey())))
                .filter(r -> r != null && r.getEnabled() != null && r.getEnabled() == 1)
                .map(assembler::assemble)
                .toList();
    }

    private DetectRun persist(Customer c, Long taskId, String label, Enums.TriggerType trigger,
                              Severity gate, RunOutcome outcome, Instant start, Instant end) {
        DetectRun run = new DetectRun();
        run.setTaskId(taskId);
        run.setCustomerId(c.getId());
        run.setLabel(label);
        run.setTriggerType(trigger.name());
        run.setStartedAt(start);
        run.setFinishedAt(end);
        run.setDurationMs(end.toEpochMilli() - start.toEpochMilli());
        run.setTotal((int) outcome.getTotal());
        run.setPassed((int) outcome.getPassed());
        run.setFailed((int) outcome.getFailed());
        run.setErrored((int) outcome.getErrored());
        run.setSkipped((int) outcome.getSkipped());
        run.setMaxSeverity(outcome.getMaxSeverity() != null ? outcome.getMaxSeverity().name() : null);
        run.setGateSeverity(gate.name());
        run.setGatePassed(outcome.gatePassed(gate) ? 1 : 0);
        run.setCreatedAt(Instant.now());
        runMapper.insert(run);

        for (CheckResultData d : outcome.getResults()) {
            CheckResultEntity e = new CheckResultEntity();
            e.setRunId(run.getId());
            e.setRuleKey(d.ruleKey());
            e.setRuleName(d.name());
            e.setCategory(d.category() != null ? d.category().name() : null);
            e.setSeverity(d.severity() != null ? d.severity().name() : null);
            e.setStatus(d.status().name());
            e.setMessage(truncate(d.message(), 1000));
            e.setMetricsJson(toJson(d.metrics()));
            e.setDurationMs(d.durationMs());
            e.setError(truncate(d.error(), 1000));
            resultMapper.insert(e);

            if (d.samples() != null && !d.samples().isEmpty()) {
                for (var sample : d.samples()) {
                    CheckViolationSample s = new CheckViolationSample();
                    s.setCheckResultId(e.getId());
                    s.setSampleJson(toJson(sample));
                    sampleMapper.insert(s);
                }
            }
        }
        log.info("检测运行完成 customer={}, run={}, 失败={}, 门禁通过={}",
                c.getId(), run.getId(), run.getFailed(), run.getGatePassed());
        return run;
    }

    private String toJson(Object o) {
        try {
            return o == null ? null : om.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
