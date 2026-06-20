package com.puxun.monitor.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "platform.security")
public class PlatformSecurityProperties {

    private Jwt jwt = new Jwt();
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();
    /** 业务库口令加密密钥(Base64, 16/24/32 字节)。生产务必通过环境变量注入。 */
    private String cipherKey = "MDEyMzQ1Njc4OWFiY2RlZg==";

    @Data
    public static class Jwt {
        private String secret;
        private long accessTtlMinutes = 120;
        private long refreshTtlDays = 7;
    }

    @Data
    public static class BootstrapAdmin {
        private boolean enabled = true;
        private String username = "admin";
        private String password = "admin123";
    }
}
