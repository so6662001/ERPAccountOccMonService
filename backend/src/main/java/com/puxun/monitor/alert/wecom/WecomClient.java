package com.puxun.monitor.alert.wecom;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 企业微信 HTTP 客户端：群机器人 Webhook 推送。自建应用(APP)消息后续接入。
 */
@Slf4j
@Component
public class WecomClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public record HttpResp(int status, String body) {}

    public HttpResp sendWebhook(String webhookUrl, String jsonPayload) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return new HttpResp(resp.statusCode(), resp.body());
        } catch (Exception e) {
            log.warn("企业微信 webhook 推送失败: {}", e.getMessage());
            return new HttpResp(-1, e.getMessage());
        }
    }
}
