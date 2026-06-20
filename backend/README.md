# 普讯科技产品应用数据监测平台 — 后端

Java 17 + Spring Boot 3.2 + MyBatis-Plus + Flyway + Spring Security(JWT) + Redis。

> 按 `docs/AI开发提示词.md` 的里程碑推进，当前已完成 **M1 地基**：工程骨架、平台库 Flyway 迁移、统一返回体/异常/枚举、认证与 RBAC（JWT）、审计切面。

## 已实现（M1）
- 统一返回体 `ApiResult` / 分页 `PageResult` / 全局异常 / TraceId。
- 共享枚举（Severity、RuleCategory、RuleType、IsolationMode 等）。
- 认证：账号密码登录、刷新令牌、`/auth/me`、权限点；JWT 过滤器；BCrypt。
- RBAC：内置角色 ADMIN/RESEARCH/FINANCE/READONLY + 角色权限点；`@PreAuthorize` 方法级鉴权。
- 审计：`@Audited` 注解 + 切面，关键写操作异步落 `audit_log`（只追加）。
- 首启自动创建管理员（`admin`，默认密码经 BCrypt，见下）。

## 本地运行
```bash
# 1) 启动依赖（平台库 + Redis）
docker compose up -d

# 2) 启动后端（首次会执行 Flyway 迁移建表 + 创建初始管理员）
export ADMIN_INIT_PWD=admin123   # 可选，默认 admin123
mvn spring-boot:run

# 3) 文档
# Swagger UI: http://localhost:8080/api/swagger-ui.html
```

环境变量：`PLATFORM_DB_HOST/PORT/NAME/USER/PWD`、`REDIS_HOST/PORT/PWD`、`JWT_SECRET`、`ADMIN_INIT_PWD`。

## 登录自测
```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
# 取返回的 accessToken：
curl -s http://localhost:8080/api/auth/me -H "Authorization: Bearer <accessToken>"
```

## 测试 / 构建
```bash
mvn test       # 运行单元测试（不依赖数据库）
mvn package    # 打包
```

## 后续里程碑
M2 数据底座（动态多数据源/客户租户）→ M3 规则核心（5 机制引擎/版本生效/模板）→ M4 运行闭环（调度/结果/门禁）→ M5 回归与告警 → M6 灰度发布 → M7 可视化 → M8 加固。
详见 `../docs/AI开发提示词.md` 与 `../docs/模块详设-*.md`。
