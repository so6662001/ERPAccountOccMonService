package com.puxun.monitor.alert.wecom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puxun.monitor.alert.domain.Alert;
import com.puxun.monitor.alert.domain.AlertChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 企业微信消息渲染：按渠道 msgType 生成 markdown/text 负载。
 */
@Component
@RequiredArgsConstructor
public class WecomMessageRenderer {

    private final ObjectMapper om;

    /** 自建应用消息负载：{touser, agentid, msgtype, markdown/text:{content}} */
    public String renderApp(AlertChannel ch, List<Alert> batch, String agentId, String toUser) {
        boolean text = "TEXT".equals(ch.getMsgType());
        StringBuilder content = new StringBuilder("账务正确性告警\n");
        for (Alert a : batch) {
            content.append("[").append(a.getSeverity()).append("] ").append(a.getTitle())
                    .append("  规则:").append(a.getRuleKey()).append("\n");
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("touser", toUser != null ? toUser : "@all");
            payload.put("agentid", agentId);
            if (text) {
                payload.put("msgtype", "text");
                payload.put("text", Map.of("content", content.toString()));
            } else {
                payload.put("msgtype", "markdown");
                payload.put("markdown", Map.of("content", content.toString()));
            }
            return om.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    public String render(AlertChannel ch, List<Alert> batch) {
        String type = ch.getMsgType() == null ? "MARKDOWN" : ch.getMsgType();
        try {
            if ("TEXT".equals(type)) {
                StringBuilder sb = new StringBuilder("【账务监测告警】\n");
                for (Alert a : batch) {
                    sb.append("[").append(a.getSeverity()).append("] ").append(a.getTitle()).append("\n");
                }
                Map<String, Object> text = new LinkedHashMap<>();
                text.put("content", sb.toString());
                if (ch.getMentionMobiles() != null && !ch.getMentionMobiles().isBlank()) {
                    text.put("mentioned_mobile_list", Arrays.asList(ch.getMentionMobiles().split(",")));
                }
                return om.writeValueAsString(Map.of("msgtype", "text", "text", text));
            }
            // 默认 markdown
            StringBuilder md = new StringBuilder("## 账务正确性告警\n");
            for (Alert a : batch) {
                md.append("> **").append(a.getSeverity()).append("** ").append(a.getTitle()).append("\n");
                md.append("> 规则：`").append(a.getRuleKey()).append("`\n");
                if (a.getContentJson() != null) {
                    md.append("> 详情：").append(a.getContentJson()).append("\n");
                }
                md.append("\n");
            }
            if (batch.size() > 3) {
                md.append("> 另有相关告警，请到平台查看\n");
            }
            return om.writeValueAsString(Map.of("msgtype", "markdown",
                    "markdown", Map.of("content", md.toString())));
        } catch (Exception e) {
            return "{\"msgtype\":\"text\",\"text\":{\"content\":\"账务监测告警(渲染失败)\"}}";
        }
    }
}
