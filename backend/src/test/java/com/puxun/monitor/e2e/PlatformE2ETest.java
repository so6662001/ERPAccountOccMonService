package com.puxun.monitor.e2e;

import com.puxun.monitor.rule.RuleService;
import com.puxun.monitor.rule.dto.RuleDtos;
import com.puxun.monitor.security.auth.AuthService;
import com.puxun.monitor.security.auth.dto.AuthDtos;
import com.puxun.monitor.tenant.CustomerService;
import com.puxun.monitor.tenant.dto.CustomerDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端集成测试：真实 MySQL 容器 + Flyway 全量迁移 + Spring 全上下文。
 * 无 Docker 环境时由 @Testcontainers(disabledWithoutDocker=true) 自动跳过。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class PlatformE2ETest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("puxun_monitor")
            .withUsername("root")
            .withPassword("root");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                MYSQL.getJdbcUrl() + "?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false");
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        // 关闭 Redis 健康依赖不影响（连接懒加载）；调度照常运行
    }

    @Autowired AuthService authService;
    @Autowired CustomerService customerService;
    @Autowired RuleService ruleService;

    @Test
    void flyway_migrated_and_admin_login_works() {
        // 首启 BootstrapAdminRunner 应已创建 admin（密码 admin123 默认）
        AuthDtos.TokenResponse token = authService.login("admin", "admin123");
        assertNotNull(token.accessToken());
        assertNotNull(token.refreshToken());
    }

    @Test
    void customer_and_rule_crud_against_real_mysql() {
        Long cid = customerService.save(new CustomerDtos.SaveCmd(
                null, "e2e_cust", "E2E 客户", "商贸", "DB_PER_CUSTOMER",
                "商贸ERP", "v3.8", "华东集群", "自然月", "2026-06", null,
                "GOLD", "99.9%", "每10分钟", "≤15m", null, "ACTIVE",
                List.of(new CustomerDtos.LedgerItem("主账簿", "中国准则", 1)),
                List.of(new CustomerDtos.CurrencyItem("CNY", 1, "系统汇率表")),
                new CustomerDtos.ConsolidationItem(0, null, null, null)));
        assertNotNull(cid);

        var detail = customerService.detail(cid);
        assertEquals("E2E 客户", detail.customer().getName());
        assertEquals(1, detail.ledgers().size());

        var rule = ruleService.save(new RuleDtos.SaveCmd(
                null, "e2e.balance.global", "全局借贷平衡", "balance", "scalar_zero", "CRITICAL",
                "{\"datasourceId\":1,\"sql\":\"SELECT 0\"}", "{\"versionExpr\":\">=v3.0\"}",
                0, null, null, null, null, 1, "初始版本"));
        assertEquals(1, rule.getCurrentVersion());

        // 再次保存 → 版本递增 + 历史
        ruleService.save(new RuleDtos.SaveCmd(
                null, "e2e.balance.global", "全局借贷平衡(改)", "balance", "scalar_zero", "CRITICAL",
                "{\"datasourceId\":1,\"sql\":\"SELECT 0\"}", "{\"versionExpr\":\">=v3.0\"}",
                0, null, null, null, null, 1, "调整"));
        assertEquals(2, ruleService.getByKey("e2e.balance.global").getCurrentVersion());
        assertEquals(2, ruleService.versions("e2e.balance.global").size());
    }
}
