package com.puxun.monitor.alert.wecom;

import com.puxun.monitor.alert.domain.Alert;
import com.puxun.monitor.alert.domain.AlertChannel;
import com.puxun.monitor.alert.domain.WecomPushLog;
import com.puxun.monitor.alert.mapper.WecomPushLogMapper;
import com.puxun.monitor.datasource.security.CipherService;
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

    public record PushResult(boolean ok, int httpStatus, String message) {}

    public PushResult push(AlertChannel ch, List<Alert> batch) {
        if (batch == null || batch.isEmpty()) {
            return new PushResult(true, 200, "无需推送");
        }
        String payload = renderer.render(ch, batch);
        WecomClient.HttpResp resp = null;
        if ("WEBHOOK".equals(ch.getType())) {
            String url = cipher.decrypt(ch.getConfigCipher());
            // 网络/5xx 重试 3 次
            for (int attempt = 1; attempt <= 3; attempt++) {
                resp = client.sendWebhook(url, payload);
                if (resp.status() >= 200 && resp.status() < 300) break;
                sleep(attempt * 200L);
            }
        } else {
            // APP 自建应用消息：后续接入 access_token 流程
            resp = new WecomClient.HttpResp(-1, "APP 通道待接入");
        }
        boolean ok = resp != null && resp.status() >= 200 && resp.status() < 300;
        log(ch, batch, payload, resp);
        return new PushResult(ok, resp == null ? -1 : resp.status(), ok ? "送达" : "推送失败");
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
