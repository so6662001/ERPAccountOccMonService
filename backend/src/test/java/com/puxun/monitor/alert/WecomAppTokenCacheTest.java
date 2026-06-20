package com.puxun.monitor.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puxun.monitor.alert.wecom.WecomAppTokenCache;
import com.puxun.monitor.alert.wecom.WecomClient;
import com.puxun.monitor.common.BizException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WecomAppTokenCacheTest {

    @Test
    void caches_token_and_fetches_once() {
        WecomClient client = Mockito.mock(WecomClient.class);
        when(client.fetchAccessToken(anyString(), anyString()))
                .thenReturn(new WecomClient.HttpResp(200, "{\"errcode\":0,\"access_token\":\"TOK\",\"expires_in\":7200}"));
        WecomAppTokenCache cache = new WecomAppTokenCache(client, new ObjectMapper());

        assertEquals("TOK", cache.get("corp1", "secret1"));
        assertEquals("TOK", cache.get("corp1", "secret1")); // 命中缓存
        verify(client, times(1)).fetchAccessToken(anyString(), anyString());
    }

    @Test
    void throws_on_errcode() {
        WecomClient client = Mockito.mock(WecomClient.class);
        when(client.fetchAccessToken(anyString(), anyString()))
                .thenReturn(new WecomClient.HttpResp(200, "{\"errcode\":40013,\"errmsg\":\"invalid corpid\"}"));
        WecomAppTokenCache cache = new WecomAppTokenCache(client, new ObjectMapper());
        assertThrows(BizException.class, () -> cache.get("bad", "secret"));
    }
}
