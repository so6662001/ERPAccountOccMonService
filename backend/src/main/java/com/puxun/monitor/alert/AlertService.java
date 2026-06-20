package com.puxun.monitor.alert;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.puxun.monitor.alert.domain.Alert;
import com.puxun.monitor.alert.domain.AlertChannel;
import com.puxun.monitor.alert.mapper.AlertChannelMapper;
import com.puxun.monitor.alert.mapper.AlertMapper;
import com.puxun.monitor.alert.wecom.WecomPushService;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import com.puxun.monitor.common.enums.Severity;
import com.puxun.monitor.result.domain.CheckResultEntity;
import com.puxun.monitor.result.domain.DetectRun;
import com.puxun.monitor.result.mapper.CheckResultMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertMapper alertMapper;
    private final AlertChannelMapper channelMapper;
    private final CheckResultMapper checkResultMapper;
    private final WecomPushService pushService;

    /** 失败/错误结果聚合为告警并按策略推送企业微信。 */
    @Transactional
    public List<Alert> raise(DetectRun run) {
        List<CheckResultEntity> problems = checkResultMapper.selectList(Wrappers.<CheckResultEntity>lambdaQuery()
                .eq(CheckResultEntity::getRunId, run.getId())
                .in(CheckResultEntity::getStatus, List.of("FAILED", "ERROR")));
        List<Alert> alerts = new ArrayList<>();
        for (CheckResultEntity r : problems) {
            Alert a = new Alert();
            a.setRunId(run.getId());
            a.setCheckResultId(r.getId());
            a.setCustomerId(run.getCustomerId());
            a.setTenantKey(run.getTenantKey());
            a.setRuleKey(r.getRuleKey());
            a.setSeverity(r.getSeverity());
            a.setTitle(r.getRuleName() != null ? r.getRuleName() : r.getRuleKey());
            a.setContentJson(r.getMessage());
            a.setStatus("PENDING");
            alertMapper.insert(a);
            alerts.add(a);
        }
        dispatch(alerts);
        return alerts;
    }

    /** 推送策略：按客户分流渠道 → 门禁级别过滤 → 去重 → 合并批量推送。 */
    public void dispatch(List<Alert> alerts) {
        Map<Long, List<Alert>> byChannel = new HashMap<>();
        Map<Long, AlertChannel> channels = new HashMap<>();

        for (Alert a : alerts) {
            AlertChannel ch = resolveChannel(a.getCustomerId());
            if (ch == null) {
                continue; // 无渠道，仅入库
            }
            Severity sev = parseSeverity(a.getSeverity());
            Severity gate = parseSeverity(ch.getGateSeverity());
            if (sev == null || gate == null || !sev.reaches(gate)) {
                a.setWecomPushStatus("SILENCED");
                alertMapper.updateById(a);
                continue;
            }
            if (isDuplicated(a, ch.getDedupWindowSec())) {
                a.setWecomPushStatus("MERGED");
                alertMapper.updateById(a);
                continue;
            }
            channels.putIfAbsent(ch.getId(), ch);
            byChannel.computeIfAbsent(ch.getId(), k -> new ArrayList<>()).add(a);
        }

        for (Map.Entry<Long, List<Alert>> e : byChannel.entrySet()) {
            AlertChannel ch = channels.get(e.getKey());
            WecomPushService.PushResult res = pushService.push(ch, e.getValue());
            String status = res.ok() ? "SENT" : "FAILED";
            for (Alert a : e.getValue()) {
                a.setWecomPushStatus(status);
                alertMapper.updateById(a);
            }
        }
    }

    private boolean isDuplicated(Alert a, Integer windowSec) {
        int w = windowSec == null ? 300 : windowSec;
        Instant since = Instant.now().minus(w, ChronoUnit.SECONDS);
        Long cnt = alertMapper.selectCount(Wrappers.<Alert>lambdaQuery()
                .eq(Alert::getCustomerId, a.getCustomerId())
                .eq(Alert::getRuleKey, a.getRuleKey())
                .eq(Alert::getWecomPushStatus, "SENT")
                .ge(Alert::getCreatedAt, since));
        return cnt != null && cnt > 0;
    }

    private AlertChannel resolveChannel(Long customerId) {
        if (customerId != null) {
            AlertChannel custom = channelMapper.selectOne(Wrappers.<AlertChannel>lambdaQuery()
                    .eq(AlertChannel::getEnabled, 1)
                    .eq(AlertChannel::getScopeType, "CUSTOMER")
                    .eq(AlertChannel::getScopeRef, String.valueOf(customerId))
                    .last("LIMIT 1"), false);
            if (custom != null) return custom;
        }
        return channelMapper.selectOne(Wrappers.<AlertChannel>lambdaQuery()
                .eq(AlertChannel::getEnabled, 1)
                .eq(AlertChannel::getScopeType, "DEFAULT")
                .last("LIMIT 1"), false);
    }

    private Severity parseSeverity(String s) {
        try { return s == null ? null : Severity.valueOf(s); } catch (Exception e) { return null; }
    }

    // ---- 处置闭环 ----

    public void claim(Long id, String user) {
        Alert a = mustGet(id);
        a.setStatus("PROCESSING");
        a.setAssignee(user);
        alertMapper.updateById(a);
    }

    public void transfer(Long id, String toUser) {
        Alert a = mustGet(id);
        a.setAssignee(toUser);
        alertMapper.updateById(a);
    }

    public void close(Long id, String user) {
        Alert a = mustGet(id);
        a.setStatus("CLOSED");
        a.setClosedAt(Instant.now());
        if (a.getAssignee() == null) a.setAssignee(user);
        alertMapper.updateById(a);
    }

    /** 人工标记误报：用于灰度发布的误报率计算，并关闭该告警。 */
    public void markFalsePositive(Long id, String user) {
        Alert a = mustGet(id);
        a.setFalsePositive(1);
        a.setStatus("CLOSED");
        a.setClosedAt(Instant.now());
        if (a.getAssignee() == null) a.setAssignee(user);
        alertMapper.updateById(a);
    }

    public IPage<Alert> query(String status, String severity, Long customerId, long page, long size) {
        return alertMapper.selectPage(new Page<>(page, size),
                Wrappers.<Alert>lambdaQuery()
                        .eq(status != null && !status.isBlank(), Alert::getStatus, status)
                        .eq(severity != null && !severity.isBlank(), Alert::getSeverity, severity)
                        .eq(customerId != null, Alert::getCustomerId, customerId)
                        .orderByDesc(Alert::getCreatedAt));
    }

    private Alert mustGet(Long id) {
        Alert a = alertMapper.selectById(id);
        if (a == null) throw new BizException(ResultCode.NOT_FOUND, "告警不存在");
        return a;
    }
}
