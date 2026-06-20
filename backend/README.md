# 普讯科技产品应用数据监测平台 — 后端

Java 17 + Spring Boot 3.2 + MyBatis-Plus + Flyway + Spring Security(JWT) + Redis。

> 按 `docs/AI开发提示词.md` 的里程碑推进。已完成 **M1 地基**、**M2 数据底座**、**M3 规则核心(引擎+版本)**。

## 进度
- M1 ✅ 工程骨架、平台库 Flyway、统一返回体/异常/枚举、认证与 RBAC(JWT)、审计切面。
- M2 ✅ 动态多数据源(只读副本/连接池隔离/探活)、AES 口令加密、JSqlParser 只读校验、只读访问门面、数据源管理、客户与租户(账簿/币种/合并口径)、自动发现租户。
- M3 ✅ 版本生效判定(VersionSpec/VersionGate)、检查引擎(5 机制执行器 + 异常隔离 + 门禁)、规则 CRUD/版本化/回滚、**规则集/模板库 + 一键套用(按版本交集下发)**。（租户聚合扫描优化作为后续性能增强）
- M4 ✅ 检测执行(范围展开 + 生效规则解析 + 引擎执行 + 落库) + 检测结果查询(运行/明细/违规样本下钻) + 检测任务 CRUD + **动态 Cron 调度** + 手动执行 + **CI 发布门禁**(返回 exitCode)。
- M5 ✅ 基线快照/审定/重置 + **回归对比**(并入运行与门禁) + 告警聚合 + **企业微信推送**(群机器人 Webhook，门禁过滤/去重/分流/重试/留痕) + 渠道 CRUD/测试 + 处置闭环(认领/转交/关闭) + 移动端告警 API。检测运行后自动触发回归与告警。
- M6 ✅ **灰度发布引擎**(分批生效、默认人工确认推进、定时观察评估、误报率/错误率超阈值自动回滚、仅版本达标目标纳入) + **模板差异对比与选择性同步**(新增/修改/弃用/本地自定义/冲突五类，冲突项强制人工逐条决定)。
- M7 ✅ **趋势与统计聚合 API**：总览 KPI、通过率趋势、失败按类别时间序列、Top 失败规则、各行业健康度、告警 MTTR、单客户健康趋势（为前端 Dashboard/Analytics 提供数据）。

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
