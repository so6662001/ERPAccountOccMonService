package com.puxun.monitor.common.enums;

import lombok.Getter;

/**
 * 检查严重级别，兼作 CI 门禁阈值。level 越大越严重。
 */
@Getter
public enum Severity {

    INFO(10),
    LOW(20),
    MEDIUM(30),
    HIGH(40),
    CRITICAL(50);

    private final int level;

    Severity(int level) {
        this.level = level;
    }

    /** 是否达到门禁级别（this >= gate）。 */
    public boolean reaches(Severity gate) {
        return this.level >= gate.level;
    }

    public static Severity max(Severity a, Severity b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.level >= b.level ? a : b;
    }
}
