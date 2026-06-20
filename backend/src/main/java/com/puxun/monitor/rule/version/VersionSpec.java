package com.puxun.monitor.rule.version;

/**
 * 生效版本表达式：支持 ">=v3.0" / "<=v9.0" / "=v4.0" / ">v3" / "<v9" /
 * 区间 "[v3.2,v9.0)" "(a,b]" "[a,b]" "(a,b)" / "*"(或空)=不限。
 * 语义化版本按数值段比较（v9 < v10）。
 */
public final class VersionSpec {

    private enum Kind { ALL, GE, GT, LE, LT, EQ, RANGE }

    private final Kind kind;
    private final String v1;
    private final String v2;
    private final boolean lowInclusive;
    private final boolean highInclusive;

    private VersionSpec(Kind kind, String v1, String v2, boolean lowInc, boolean highInc) {
        this.kind = kind; this.v1 = v1; this.v2 = v2;
        this.lowInclusive = lowInc; this.highInclusive = highInc;
    }

    public static VersionSpec parse(String expr) {
        if (expr == null || expr.isBlank() || "*".equals(expr.trim())) {
            return new VersionSpec(Kind.ALL, null, null, false, false);
        }
        String s = expr.trim();
        if (s.startsWith("[") || s.startsWith("(")) {
            boolean lowInc = s.startsWith("[");
            boolean highInc = s.endsWith("]");
            String inner = s.substring(1, s.length() - 1);
            String[] parts = inner.split(",");
            if (parts.length != 2) throw new IllegalArgumentException("非法区间表达式: " + expr);
            return new VersionSpec(Kind.RANGE, norm(parts[0]), norm(parts[1]), lowInc, highInc);
        }
        if (s.startsWith(">=")) return new VersionSpec(Kind.GE, norm(s.substring(2)), null, false, false);
        if (s.startsWith("<=")) return new VersionSpec(Kind.LE, norm(s.substring(2)), null, false, false);
        if (s.startsWith(">"))  return new VersionSpec(Kind.GT, norm(s.substring(1)), null, false, false);
        if (s.startsWith("<"))  return new VersionSpec(Kind.LT, norm(s.substring(1)), null, false, false);
        if (s.startsWith("="))  return new VersionSpec(Kind.EQ, norm(s.substring(1)), null, false, false);
        // 裸版本号视为等于
        return new VersionSpec(Kind.EQ, norm(s), null, false, false);
    }

    public boolean matches(String deployVersion) {
        if (kind == Kind.ALL) return true;
        if (deployVersion == null || deployVersion.isBlank()) return false;
        String v = norm(deployVersion);
        int c1 = (v1 == null) ? 0 : compare(v, v1);
        return switch (kind) {
            case GE -> c1 >= 0;
            case GT -> c1 > 0;
            case LE -> c1 <= 0;
            case LT -> c1 < 0;
            case EQ -> c1 == 0;
            case RANGE -> {
                int low = compare(v, v1);
                int high = compare(v, v2);
                boolean okLow = lowInclusive ? low >= 0 : low > 0;
                boolean okHigh = highInclusive ? high <= 0 : high < 0;
                yield okLow && okHigh;
            }
            default -> true;
        };
    }

    private static String norm(String v) {
        String t = v.trim();
        if (t.startsWith("v") || t.startsWith("V")) t = t.substring(1);
        return t;
    }

    /** 语义化版本比较：按 '.' 分段数值比较，缺段补 0。 */
    public static int compare(String a, String b) {
        String[] pa = a.split("[.\\-+]");
        String[] pb = b.split("[.\\-+]");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            long x = seg(pa, i);
            long y = seg(pb, i);
            if (x != y) return Long.compare(x, y);
        }
        return 0;
    }

    private static long seg(String[] parts, int i) {
        if (i >= parts.length) return 0;
        try {
            return Long.parseLong(parts[i].replaceAll("\\D", "").isEmpty() ? "0" : parts[i].replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
