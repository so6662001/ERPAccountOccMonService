package com.puxun.monitor.datasource.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 违规样本脱敏：对疑似敏感字段（含 name/mobile/phone/idcard/card/account/bank/email/pwd/secret 等关键字）做掩码。
 */
public final class SampleMasker {

    private SampleMasker() {}

    private static final String[] SENSITIVE = {
            "name", "mobile", "phone", "idcard", "id_card", "card", "account",
            "bank", "email", "pwd", "password", "secret", "token"
    };

    public static List<Map<String, Object>> mask(List<Map<String, Object>> rows) {
        if (rows == null) return List.of();
        for (Map<String, Object> row : rows) {
            maskRow(row);
        }
        return rows;
    }

    private static void maskRow(Map<String, Object> row) {
        for (Map.Entry<String, Object> e : new LinkedHashMap<>(row).entrySet()) {
            if (isSensitive(e.getKey()) && e.getValue() != null) {
                row.put(e.getKey(), maskValue(e.getValue().toString()));
            }
        }
    }

    private static boolean isSensitive(String key) {
        if (key == null) return false;
        String k = key.toLowerCase(Locale.ROOT);
        for (String s : SENSITIVE) {
            if (k.contains(s)) return true;
        }
        return false;
    }

    /** 保留首尾，中间用 * 掩码。 */
    public static String maskValue(String v) {
        if (v == null) return null;
        int n = v.length();
        if (n <= 2) return "*".repeat(n);
        if (n <= 6) return v.charAt(0) + "*".repeat(n - 2) + v.charAt(n - 1);
        return v.substring(0, 2) + "****" + v.substring(n - 2);
    }
}
