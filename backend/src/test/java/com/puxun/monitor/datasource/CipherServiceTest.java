package com.puxun.monitor.datasource;

import com.puxun.monitor.datasource.security.CipherService;
import com.puxun.monitor.security.config.PlatformSecurityProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CipherServiceTest {

    private CipherService newService() {
        // 16 字节 AES key 的 Base64
        PlatformSecurityProperties props = new PlatformSecurityProperties();
        props.setCipherKey("MDEyMzQ1Njc4OWFiY2RlZg==");
        return new CipherService(props);
    }

    @Test
    void roundtrip() {
        CipherService c = newService();
        String secret = "p@ssw0rd-业务库口令";
        String enc = c.encrypt(secret);
        assertNotEquals(secret, enc);
        assertEquals(secret, c.decrypt(enc));
    }

    @Test
    void different_iv_each_time() {
        CipherService c = newService();
        assertNotEquals(c.encrypt("same"), c.encrypt("same")); // 随机 IV，密文不同
    }

    @Test
    void null_safe() {
        CipherService c = newService();
        assertNull(c.encrypt(null));
        assertNull(c.decrypt(null));
    }
}
