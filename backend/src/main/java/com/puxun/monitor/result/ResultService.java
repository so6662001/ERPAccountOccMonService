package com.puxun.monitor.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.result.domain.CheckResultEntity;
import com.puxun.monitor.result.domain.CheckViolationSample;
import com.puxun.monitor.result.domain.DetectRun;
import com.puxun.monitor.result.mapper.CheckResultMapper;
import com.puxun.monitor.result.mapper.CheckViolationSampleMapper;
import com.puxun.monitor.result.mapper.DetectRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final DetectRunMapper runMapper;
    private final CheckResultMapper resultMapper;
    private final CheckViolationSampleMapper sampleMapper;

    public record RunDetailVO(DetectRun run, List<ResultItem> results) {}
    public record ResultItem(CheckResultEntity result, List<CheckViolationSample> samples) {}

    public IPage<DetectRun> pageRuns(Long customerId, long page, long size) {
        return runMapper.selectPage(new Page<>(page, size),
                Wrappers.<DetectRun>lambdaQuery()
                        .eq(customerId != null, DetectRun::getCustomerId, customerId)
                        .orderByDesc(DetectRun::getStartedAt));
    }

    public RunDetailVO detail(Long runId, String status, String category) {
        DetectRun run = runMapper.selectById(runId);
        if (run == null) throw new BizException(ResultCode.NOT_FOUND, "运行不存在");
        List<CheckResultEntity> results = resultMapper.selectList(Wrappers.<CheckResultEntity>lambdaQuery()
                .eq(CheckResultEntity::getRunId, runId)
                .eq(status != null && !status.isBlank(), CheckResultEntity::getStatus, status)
                .eq(category != null && !category.isBlank(), CheckResultEntity::getCategory, category));
        List<ResultItem> items = results.stream().map(r -> {
            List<CheckViolationSample> samples = "FAILED".equals(r.getStatus())
                    ? sampleMapper.selectList(Wrappers.<CheckViolationSample>lambdaQuery()
                        .eq(CheckViolationSample::getCheckResultId, r.getId()))
                    : List.of();
            return new ResultItem(r, samples);
        }).toList();
        return new RunDetailVO(run, items);
    }
}
