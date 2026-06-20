package com.puxun.monitor.alert.wecom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 企业微信自建应用 access_token 缓存：按 corpId+agentId 缓存，提前 200s 刷新。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WecomAppTokenCache {

    private final WecomClient client;
    private final ObjectMapper om;

    private record Token(String value, Instant expireAt) {}

    private final ConcurrentHashMap<String, Token> cache = new ConcurrentHashMap<>();

    public String get(String corpId, String corpSecret) {
        String key = corpId + "#" + Integer.toHexString(corpSecret.hashCode());
        Token t = cache.get(key);
        if (t != null && Instant.now().isBefore(t.expireAt())) {
            return t.value();
        }
        return refresh(key, corpId, corpSecret);
    }

    private synchronized String refresh(String key, String corpId, String corpSecret) {
        Token t = cache.get(key);
        if (t != null && Instant.now().isBefore(t.expireAt())) {
            return t.value();   // 双检，避免并发重复刷新
        }
        WecomClient.HttpResp resp = client.fetchAccessToken(corpId, corpSecret);
        try {
            JsonNode n = om.readTree(resp.body());
            int errcode = n.path("errcode").asInt(-1);
            if (errcode != 0) {
                throw new BizException(ResultCode.BIZ_ERROR, "获取 access_token 失败: " + n.path("errmsg").asText());
            }
            String token = n.path("access_token").asText();
            long expiresIn = n.path("expires_in").asLong(7200);
            // 提前 200s 过期，避免边界失效
            Instant expireAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 200));
            cache.put(key, new Token(token, expireAt));
            return token;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.BIZ_ERROR, "解析 access_token 响应失败: " + e.getMessage());
        }
    }
}
