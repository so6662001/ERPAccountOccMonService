package com.puxun.monitor.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puxun.monitor.alert.domain.Alert;
import com.puxun.monitor.alert.domain.AlertChannel;
import com.puxun.monitor.alert.wecom.WecomMessageRenderer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WecomMessageRendererTest {

    private final WecomMessageRenderer renderer = new WecomMessageRenderer(new ObjectMapper());

    private Alert alert(String sev, String title, String key) {
        Alert a = new Alert();
        a.setSeverity(sev);
        a.setTitle(title);
        a.setRuleKey(key);
        a.setContentJson("差额 -1200");
        return a;
    }

    @Test
    void markdown_payload() {
        AlertChannel ch = new AlertChannel();
        ch.setMsgType("MARKDOWN");
        String p = renderer.render(ch, List.of(alert("CRITICAL", "凭证借贷不平", "balance.voucher_each")));
        assertTrue(p.contains("\"msgtype\":\"markdown\""));
        assertTrue(p.contains("balance.voucher_each"));
        assertTrue(p.contains("CRITICAL"));
    }

    @Test
    void text_payload_with_mentions() {
        AlertChannel ch = new AlertChannel();
        ch.setMsgType("TEXT");
        ch.setMentionMobiles("13800000000,13900000000");
        String p = renderer.render(ch, List.of(alert("HIGH", "应付勾稽错", "reconciliation.ap_control")));
        assertTrue(p.contains("\"msgtype\":\"text\""));
        assertTrue(p.contains("mentioned_mobile_list"));
        assertTrue(p.contains("13800000000"));
    }
}
