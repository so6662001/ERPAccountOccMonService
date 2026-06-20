package com.puxun.monitor.alert.wecom;

import com.puxun.monitor.alert.domain.Alert;
import com.puxun.monitor.alert.domain.AlertChannel;
import com.puxun.monitor.alert.domain.WecomPushLog;
import com.puxun.monitor.alert.mapper.WecomPushLogMapper;
import com.puxun.monitor.datasource.security.CipherService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 企业微信推送：渲染 → 发送(重试) → 留痕。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WecomPushService {

    private final WecomClient client;
    private final WecomMessageRenderer renderer;
    private final WecomPushLogMapper logMapper;
    private final CipherService cipher;
    private final WecomAppTokenCache tokenCache;
    private final ObjectMapper om;

    public record PushResult(boolean ok, int httpStatus, String message) {}

    public PushResult push(AlertChannel ch, List<Alert> batch) {
        if (batch == null || batch.isEmpty()) {
            return new PushResult(true, 200, "无需推送");
        }
        Sent sent = "APP".equals(ch.getType()) ? pushApp(ch, batch) : pushWebhook(ch, batch);
        boolean ok = sent.resp != null && sent.resp.status() >= 200 && sent.resp.status() < 300;
        log(ch, batch, sent.payload, sent.resp);
        return new PushResult(ok, sent.resp == null ? -1 : sent.resp.status(), ok ? "送达" : "推送失败");
    }

    private record Sent(String payload, WecomClient.HttpResp resp) {}

    private Sent pushWebhook(AlertChannel ch, List<Alert> batch) {
        String payload = renderer.render(ch, batch);
        String url = cipher.decrypt(ch.getConfigCipher());
        WecomClient.HttpResp resp = new WecomClient.HttpResp(-1, "init");
        for (int attempt = 1; attempt <= 3; attempt++) {       // 网络/5xx 重试 3 次
            resp = client.sendWebhook(url, payload);
            if (resp.status() >= 200 && resp.status() < 300) break;
            sleep(attempt * 200L);
        }
        return new Sent(payload, resp);
    }

    /** 自建应用推送：解析配置 → 取 token(缓存) → 发送应用消息。 */
    private Sent pushApp(AlertChannel ch, List<Alert> batch) {
        try {
            JsonNode cfg = om.readTree(cipher.decrypt(ch.getConfigCipher()));
            String corpId = cfg.path("corpId").asText();
            String corpSecret = cfg.path("corpSecret").asText();
            String agentId = cfg.path("agentId").asText();
            String toUser = cfg.hasNonNull("toUser") ? cfg.get("toUser").asText() : "@all";
            String token = tokenCache.get(corpId, corpSecret);
            String payload = renderer.renderApp(ch, batch, agentId, toUser);
            WecomClient.HttpResp resp = new WecomClient.HttpResp(-1, "init");
            for (int attempt = 1; attempt <= 3; attempt++) {
                resp = client.sendAppMessage(token, payload);
                if (resp.status() >= 200 && resp.status() < 300) break;
                sleep(attempt * 200L);
            }
            return new Sent(payload, resp);
        } catch (Exception e) {
            return new Sent("{\"error\":\"app\"}", new WecomClient.HttpResp(-1, e.getMessage()));
        }
    }

    public PushResult test(AlertChannel ch) {
        Alert demo = new Alert();
        demo.setSeverity("INFO");
        demo.setTitle("企业微信通道测试消息");
        demo.setRuleKey("test.ping");
        return push(ch, List.of(demo));
    }

    private void log(AlertChannel ch, List<Alert> batch, String payload, WecomClient.HttpResp resp) {
        WecomPushLog l = new WecomPushLog();
        l.setAlertIds(batch.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(",")));
        l.setChannelId(ch.getId());
        l.setPayloadJson(payload);
        l.setHttpStatus(resp == null ? -1 : resp.status());
        l.setResult(resp == null ? "no-resp" : (resp.status() >= 200 && resp.status() < 300 ? "OK" : "FAIL"));
        l.setPushedAt(Instant.now());
        logMapper.insert(l);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
