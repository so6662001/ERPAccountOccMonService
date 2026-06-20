package com.puxun.monitor.common;

import com.puxun.monitor.common.enums.Severity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeverityTest {

    @Test
    void reaches_gate() {
        assertTrue(Severity.CRITICAL.reaches(Severity.HIGH));
        assertTrue(Severity.HIGH.reaches(Severity.HIGH));
        assertFalse(Severity.MEDIUM.reaches(Severity.HIGH));
    }

    @Test
    void max_picks_higher() {
        assertEquals(Severity.CRITICAL, Severity.max(Severity.LOW, Severity.CRITICAL));
        assertEquals(Severity.HIGH, Severity.max(Severity.HIGH, null));
        assertEquals(Severity.MEDIUM, Severity.max(null, Severity.MEDIUM));
        assertNull(Severity.max(null, null));
    }
}
