package com.puxun.monitor.analytics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsServiceTest {

    @Test
    void pass_rate_pct() {
        assertEquals(new BigDecimal("99.22"), AnalyticsService.passRatePct(8571, 8638).setScale(2));
        assertEquals(new BigDecimal("100.00"), AnalyticsService.passRatePct(10, 10).setScale(2));
        assertEquals(BigDecimal.ZERO, AnalyticsService.passRatePct(0, 0));
        assertEquals(new BigDecimal("0.00"), AnalyticsService.passRatePct(0, 5).setScale(2));
    }
}
