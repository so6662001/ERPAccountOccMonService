package com.puxun.monitor.engine;

import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.common.enums.Severity;
import com.puxun.monitor.engine.model.EngineModels.CheckResultData;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次运行的结果与汇总。
 */
@Getter
public class RunOutcome {

    private final List<CheckResultData> results = new ArrayList<>();
    private long total, passed, failed, errored, skipped;
    private Severity maxSeverity;

    public void add(CheckResultData r) {
        results.add(r);
    }

    public void addAll(List<CheckResultData> rs) {
        results.addAll(rs);
    }

    public RunOutcome computeSummary() {
        total = results.size();
        passed = count(Enums.CheckStatus.PASSED);
        failed = count(Enums.CheckStatus.FAILED);
        errored = count(Enums.CheckStatus.ERROR);
        skipped = count(Enums.CheckStatus.SKIPPED);
        maxSeverity = results.stream()
                .filter(r -> r.status().isProblem())
                .map(CheckResultData::severity)
                .reduce(null, Severity::max);
        return this;
    }

    private long count(Enums.CheckStatus s) {
        return results.stream().filter(r -> r.status() == s).count();
    }

    /** 是否通过门禁：不存在 >= gate 的问题。 */
    public boolean gatePassed(Severity gate) {
        return maxSeverity == null || !maxSeverity.reaches(gate);
    }
}
