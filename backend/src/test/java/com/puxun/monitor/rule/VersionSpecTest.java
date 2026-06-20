package com.puxun.monitor.rule;

import com.puxun.monitor.rule.version.VersionSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionSpecTest {

    @Test
    void ge() {
        VersionSpec s = VersionSpec.parse(">=v3.0");
        assertTrue(s.matches("v3.0"));
        assertTrue(s.matches("v3.8.2"));
        assertFalse(s.matches("v2.9"));
    }

    @Test
    void semantic_numeric_compare() {
        // v9 < v10 必须按数值，而非字典序
        assertTrue(VersionSpec.compare("v10.0", "v9.0") > 0);
        assertTrue(VersionSpec.parse(">=v4.0").matches("v10.1"));
    }

    @Test
    void range() {
        VersionSpec s = VersionSpec.parse("[v3.2,v9.0)");
        assertFalse(s.matches("v3.1"));
        assertTrue(s.matches("v3.2"));
        assertTrue(s.matches("v8.9"));
        assertFalse(s.matches("v9.0"));   // 上界开区间
    }

    @Test
    void all_and_eq() {
        assertTrue(VersionSpec.parse("*").matches("anything-1.0"));
        assertTrue(VersionSpec.parse(null).matches("v1"));
        assertTrue(VersionSpec.parse("=v4.0").matches("v4.0"));
        assertFalse(VersionSpec.parse("=v4.0").matches("v4.1"));
    }

    @Test
    void empty_deploy_version_not_match_bounded() {
        assertFalse(VersionSpec.parse(">=v3.0").matches(null));
        assertTrue(VersionSpec.parse("*").matches(null));
    }
}
