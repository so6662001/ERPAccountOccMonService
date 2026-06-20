package com.puxun.monitor.schedule;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.common.enums.Severity;
import com.puxun.monitor.result.DetectionService;
import com.puxun.monitor.result.domain.DetectRun;
import com.puxun.monitor.tenant.domain.Customer;
import com.puxun.monitor.tenant.mapper.CustomerMapper;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * CI 发布门禁：对目标客户执行检测，聚合门禁结论，供流水线判定是否阻断发布。
 */
@Service
@RequiredArgsConstructor
public class CiGateService {

    private final DetectionService detectionService;
    private final CustomerMapper customerMapper;

    public record GateRequest(@NotEmpty List<Long> customerIds, String gateSeverity, String label, String productLine) {}

    public record GateResult(boolean passed, String maxSeverity, long failedRuns,
                             int exitCode, List<DetectRun> runs) {}

    public GateResult run(GateRequest req) {
        Severity gate = req.gateSeverity() != null ? Severity.valueOf(req.gateSeverity()) : Severity.CRITICAL;
        List<Long> ids = (req.customerIds() != null && !req.customerIds().isEmpty())
                ? req.customerIds()
                : customerMapper.selectList(Wrappers.<Customer>lambdaQuery()
                        .eq(req.productLine() != null, Customer::getProductLine, req.productLine())
                        .eq(Customer::getStatus, "ACTIVE")).stream().map(Customer::getId).toList();

        List<DetectRun> runs = new ArrayList<>();
        boolean passed = true;
        Severity max = null;
        long failedRuns = 0;
        for (Long cid : ids) {
            DetectRun run = detectionService.runForCustomer(cid, null,
                    req.label() != null ? req.label() : "ci-gate", Enums.TriggerType.CI_GATE, gate);
            runs.add(run);
            if (run.getGatePassed() != null && run.getGatePassed() == 0) {
                passed = false;
                failedRuns++;
            }
            if (run.getMaxSeverity() != null) {
                max = Severity.max(max, Severity.valueOf(run.getMaxSeverity()));
            }
        }
        return new GateResult(passed, max != null ? max.name() : null, failedRuns, passed ? 0 : 1, runs);
    }
}
