package com.puxun.monitor.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puxun.monitor.audit.Audited;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.common.enums.Severity;
import com.puxun.monitor.result.DetectionService;
import com.puxun.monitor.result.domain.DetectRun;
import com.puxun.monitor.schedule.domain.DetectTask;
import com.puxun.monitor.schedule.mapper.DetectTaskMapper;
import com.puxun.monitor.tenant.domain.Customer;
import com.puxun.monitor.tenant.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final DetectTaskMapper taskMapper;
    private final CustomerMapper customerMapper;
    private final DetectionService detectionService;
    private final ObjectMapper om;

    public IPage<DetectTask> page(long page, long size) {
        return taskMapper.selectPage(new Page<>(page, size),
                Wrappers.<DetectTask>lambdaQuery().orderByDesc(DetectTask::getUpdatedAt));
    }

    public DetectTask get(Long id) {
        DetectTask t = taskMapper.selectById(id);
        if (t == null) throw new BizException(ResultCode.NOT_FOUND, "任务不存在");
        return t;
    }

    public List<DetectTask> enabledCronTasks() {
        return taskMapper.selectList(Wrappers.<DetectTask>lambdaQuery()
                .eq(DetectTask::getTriggerType, Enums.TriggerType.CRON.name())
                .eq(DetectTask::getEnabled, 1));
    }

    @Audited(action = "EDIT_TASK")
    public DetectTask save(DetectTask t) {
        if (t.getConcurrency() == null) t.setConcurrency(4);
        if (t.getEnabled() == null) t.setEnabled(1);
        if (t.getGateSeverity() == null) t.setGateSeverity(Severity.HIGH.name());
        if (t.getStatus() == null) t.setStatus("IDLE");
        if (t.getId() != null) taskMapper.updateById(t); else taskMapper.insert(t);
        return t;
    }

    public void setEnabled(Long id, boolean enabled) {
        DetectTask t = get(id);
        t.setEnabled(enabled ? 1 : 0);
        t.setStatus(enabled ? "IDLE" : "PAUSED");
        taskMapper.updateById(t);
    }

    /** 立即执行任务：展开范围 → 逐客户检测。 */
    @Audited(action = "RUN_TASK")
    public List<DetectRun> runNow(Long taskId, Enums.TriggerType trigger) {
        DetectTask t = get(taskId);
        t.setStatus("RUNNING");
        t.setLastRunAt(Instant.now());
        taskMapper.updateById(t);

        Severity gate = Severity.valueOf(t.getGateSeverity());
        List<Long> customerIds = expandCustomers(t.getScopeJson());
        List<DetectRun> runs = new ArrayList<>();
        for (Long cid : customerIds) {
            try {
                runs.add(detectionService.runForCustomer(cid, taskId, t.getName(), trigger, gate));
            } catch (Exception e) {
                log.warn("任务 {} 客户 {} 执行失败: {}", taskId, cid, e.getMessage());
            }
        }
        t.setStatus("IDLE");
        taskMapper.updateById(t);
        return runs;
    }

    /** 解析 scope_json 得到目标客户；空/无 customerIds 时取全部 ACTIVE 客户（可按 industry/isolationMode 过滤）。 */
    public List<Long> expandCustomers(String scopeJson) {
        try {
            if (scopeJson != null && !scopeJson.isBlank()) {
                JsonNode n = om.readTree(scopeJson);
                if (n.hasNonNull("customerIds") && n.get("customerIds").isArray()) {
                    List<Long> ids = new ArrayList<>();
                    n.get("customerIds").forEach(x -> ids.add(x.asLong()));
                    if (!ids.isEmpty()) return ids;
                }
                String industry = n.hasNonNull("industry") ? n.get("industry").asText() : null;
                String mode = n.hasNonNull("isolationMode") ? n.get("isolationMode").asText() : null;
                return customerMapper.selectList(Wrappers.<Customer>lambdaQuery()
                                .eq(Customer::getStatus, "ACTIVE")
                                .eq(industry != null, Customer::getIndustry, industry)
                                .eq(mode != null, Customer::getIsolationMode, mode))
                        .stream().map(Customer::getId).toList();
            }
        } catch (Exception e) {
            log.warn("解析任务范围失败: {}", e.getMessage());
        }
        return customerMapper.selectList(Wrappers.<Customer>lambdaQuery().eq(Customer::getStatus, "ACTIVE"))
                .stream().map(Customer::getId).toList();
    }
}
