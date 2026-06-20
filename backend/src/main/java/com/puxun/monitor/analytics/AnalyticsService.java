package com.puxun.monitor.analytics;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.puxun.monitor.alert.domain.Alert;
import com.puxun.monitor.alert.mapper.AlertMapper;
import com.puxun.monitor.tenant.domain.Customer;
import com.puxun.monitor.tenant.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 统计聚合服务：为 Dashboard / 趋势分析提供数据。
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsMapper mapper;
    private final CustomerMapper customerMapper;
    private final AlertMapper alertMapper;

    public Map<String, Object> overview() {
        Instant since1d = Instant.now().minus(1, ChronoUnit.DAYS);
        Map<String, Object> exec = mapper.execSummary(since1d);
        long total = asLong(exec.get("total"));
        long passed = asLong(exec.get("passed"));

        long customers = customerMapper.selectCount(Wrappers.<Customer>lambdaQuery());
        long pendingAlerts = alertMapper.selectCount(Wrappers.<Alert>lambdaQuery().eq(Alert::getStatus, "PENDING"));

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("customers", customers);
        r.put("todayExecutions", total);
        r.put("todayPassRate", passRate(passed, total));
        r.put("pendingAlerts", pendingAlerts);
        r.put("alertBySeverity", mapper.openAlertBySeverity());
        return r;
    }

    public List<Map<String, Object>> passRateTrend(int days) {
        List<Map<String, Object>> rows = mapper.passRateTrend(since(days));
        rows.forEach(m -> m.put("passRate", passRate(asLong(m.get("passed")), asLong(m.get("total")))));
        return rows;
    }

    public List<Map<String, Object>> failuresByCategory(int days) {
        return mapper.failuresByCategory(since(days));
    }

    public List<Map<String, Object>> topFailingRules(int days, int limit) {
        return mapper.topFailingRules(since(days), limit);
    }

    public List<Map<String, Object>> industryHealth(int days) {
        List<Map<String, Object>> rows = mapper.industryHealth(since(days));
        rows.forEach(m -> m.put("passRate", passRate(asLong(m.get("passed")), asLong(m.get("total")))));
        return rows;
    }

    public Double mttrMinutes(int days) {
        Double v = mapper.mttrMinutes(since(days));
        return v == null ? 0d : Math.round(v * 10) / 10d;
    }

    public List<Map<String, Object>> customerTrend(Long customerId, int days) {
        List<Map<String, Object>> rows = mapper.customerTrend(customerId, since(days));
        rows.forEach(m -> m.put("passRate", passRate(asLong(m.get("passed")), asLong(m.get("total")))));
        return rows;
    }

    private Instant since(int days) {
        return Instant.now().minus(Math.max(1, days), ChronoUnit.DAYS);
    }

    private BigDecimal passRate(long passed, long total) {
        return passRatePct(passed, total);
    }

    /** 通过率百分比（保留两位）。 */
    public static BigDecimal passRatePct(long passed, long total) {
        if (total <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(passed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private long asLong(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0; }
    }
}
