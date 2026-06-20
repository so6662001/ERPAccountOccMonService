package com.puxun.monitor.datasource;

import com.puxun.monitor.datasource.security.SampleMasker;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SampleMaskerTest {

    @Test
    void masks_sensitive_keys_only() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("customer_name", "张三丰");
        row.put("mobile", "13800001111");
        row.put("amount", "1200.00");
        row.put("voucher_no", "V20260620-088");

        List<Map<String, Object>> out = SampleMasker.mask(new ArrayList<>(List.of(row)));
        Map<String, Object> r = out.get(0);
        assertNotEquals("张三丰", r.get("customer_name"));   // 姓名被掩码
        assertNotEquals("13800001111", r.get("mobile"));     // 手机号被掩码
        assertEquals("1200.00", r.get("amount"));            // 金额不掩码
        assertEquals("V20260620-088", r.get("voucher_no"));  // 单号不掩码
    }

    @Test
    void mask_value_keeps_head_tail() {
        assertEquals("13****11", SampleMasker.maskValue("13800001111"));
        assertEquals("**", SampleMasker.maskValue("ab"));
    }
}
