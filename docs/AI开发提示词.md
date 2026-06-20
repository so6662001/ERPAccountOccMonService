# 普讯科技产品应用数据监测平台 — Cursor AI 开发提示词包

> 用途：把本文件的提示词分模块复制进 Cursor，驱动 AI 完成开发。文档保证**功能完整、逻辑不缺失**：每个模块都给出"目标 → 实现要点 → 接口/数据 → 逻辑完整性检查清单（验收）"。
>
> 技术栈：**后端 Java 17 + Spring Boot 3.x**，**前端 Vue3 + TypeScript + Element Plus + Pinia + Vite + ECharts**。
>
> 配套：高保真原型在 `prototype/`（22 页，从 `login.html` 进入）；账务校验方法论在 `docs/ARCHITECTURE.md`；可运行的校验引擎参考实现在 `erp_acc_monitor/`（Python，仅供逻辑参考）。
>
> 项目规则（Cursor 自动加载）：`.cursor/rules/`（总纲、只读安全与多租户、后端、前端）。
> 核心模块方法/类级详设：`docs/模块详设-规则引擎.md`、`docs/模块详设-灰度发布引擎.md`。

---

## 0. 如何使用本提示词包

1. 先把「§1 总纲提示词」作为**项目级系统提示**贴入 Cursor（或放进 `.cursor/rules`），让 AI 始终遵循。
2. 再按「§9 建议开发顺序」逐模块，把对应的「模块提示词」连同「逻辑完整性检查清单」一起发给 AI。
3. 每完成一个模块，让 AI 对照该模块的检查清单**自检并补齐缺失逻辑**，再进入下一个。
4. 涉及多模块协作时，引用「§3 数据模型」与「§4 通用约定」，确保字段/状态一致。

---

## 1. 总纲提示词（项目级系统提示，务必先贴）

```
你是资深全栈工程师，正在开发「普讯科技产品应用数据监测平台」。

# 产品一句话定位
在系统持续迭代的前提下，守住 ERP 账务数据的正确性：把"必须永远成立的账务恒等式"形式化为可执行的检查规则，在生产持续巡检 + 迭代发布前回归门禁两个时机校验，发现问题即按级别推送企业微信，将研发迭代引入的账务错误挡在生产之外。平台面向云化多客户/多租户场景。

# 必须始终遵守的核心原则
1. 只读安全：平台对业务库仅做 SELECT，绝不写入业务库；优先连只读副本/备库；连接池按客户隔离限流。平台自身数据写入平台库（独立）。
2. 多租户两种隔离模式都要支持：
   - 独立数据库模式：每客户一套独立库（MySQL/Oracle/SQLServer/达梦/PostgreSQL）。
   - 租户隔离模式：同一物理库内按 tenant_id 隔离；查询必须自动注入 tenant 过滤。
3. 高效检测：租户隔离模式优先用"单条聚合 SQL 跨全租户扫描 + 仅对异常租户下钻明细"，避免逐租户 N 次查询；独立库按客户并行调度。
4. 规则与产品版本挂钩：每条规则可声明 适用产品线 + 生效版本表达式(如 >=v4.0 / [v3.2,v9.0)) + 适用服务器；执行前按客户当前部署版本判定是否生效（旧版本不误报、新规则随升级自动生效）。
5. 声明式规则：规则以声明式配置(JSON/YAML)落库，支持 5 种检查机制(见下)，业务人员无需写代码即可新增/修改。
6. 全程留痕：规则变更、模板套用、灰度发布、基线重置等关键操作写入不可篡改的审计日志。
7. 金额比较一律带容差(默认 0.005)，吸收浮点误差；金额用 BigDecimal/DECIMAL，禁止用 double 做账务比较。

# 5 种检查机制(覆盖绝大多数账务约束)
- scalar_zero：某 SQL 标量必须约等于 0（如 全局 Σ借−Σ贷=0）。
- scalar_equality：两段 SQL 标量必须相等（如 应收明细合计=应收总账余额）。
- scalar_range：某 SQL 标量必须落在[min,max]（如 单位成本>=0；也作不变量指标载体）。
- rows_empty：违规明细查询必须返回 0 行（如 逐凭证不平、孤儿单据、连续性断裂；失败时抽样返回违规记录）。
- cross_system_scalar_equality：跨两个数据源的标量必须相等（如 订单系统应收=财务系统应收）。

# 账务约束类别(规则 category)
平衡 balance / 勾稽 reconciliation / 连续性 continuity / 成本 cost / 完整性 integrity / 跨系统 cross_system / 迭代回归 regression。

# 严重级别(兼作 CI 门禁阈值，数值越大越严重)
INFO < LOW < MEDIUM < HIGH < CRITICAL。门禁：出现 >= 门禁级别的失败即判定整体失败/阻断发布。

# 技术栈与规范
- 后端：Java 17、Spring Boot 3.x、Spring Web、Spring Security、MyBatis-Plus(或 JPA)、Quartz/XXL-Job 调度、Flyway 迁移、Redis(缓存/分布式锁)、SpringDoc OpenAPI。多业务数据源用动态 DataSource(AbstractRoutingDataSource) + HikariCP，按客户/租户运行时路由。
- 前端：Vue3 + TypeScript + Vite + Element Plus + Pinia + Vue Router + Axios + ECharts；UI 还原 prototype/ 各页面（深色侧边栏 + Element Plus 蓝）。
- 统一返回体 ApiResult<T> { code, message, data, traceId }；分页 PageResult<T> { records, total, page, size }。
- 时间用 UTC 存储、前端本地化展示；ID 用雪花/UUID；枚举用字符串常量。
- 代码：分层(controller/service/mapper/domain/dto)，DTO 与实体分离，关键逻辑写单元测试。

# 输出要求
- 编写完整可运行代码，不要省略关键逻辑；补齐异常处理、边界条件、空值、并发与事务。
- 遇到与上述原则冲突的需求，先指出风险再实现。
- 每完成一个模块，对照我提供的"逻辑完整性检查清单"逐条自检，列出已满足/缺失项并补齐。
```

---

## 2. 技术架构与工程结构

```
请按以下结构初始化工程（monorepo 或前后端分仓均可）：

backend/ (Spring Boot)
  ├─ common/        统一返回体、异常、分页、枚举、工具
  ├─ security/      认证(账号密码+企业微信扫码)、RBAC、JWT、审计切面
  ├─ datasource/    平台库配置 + 业务库动态多数据源(路由/只读/连接池/探活)
  ├─ tenant/        客户与租户(独立库/租户隔离)、行业、会计期间、SLA、账簿/币种/合并口径
  ├─ rule/          规则定义、规则集、版本、模板库、版本生效判定
  ├─ engine/        检查引擎(5 种机制)、租户聚合扫描、违规样本抽样、容差
  ├─ schedule/      任务调度(cron/事件/CI 门禁/手动)、并发与错峰、执行历史
  ├─ result/        检测运行与结果、违规明细、报表导出
  ├─ baseline/      基线快照、回归对比、审定
  ├─ rollout/       灰度/分批发布引擎、观察期、自动回滚、模板差异与选择性同步
  ├─ alert/         告警聚合、企业微信推送(群机器人/自建应用)、移动端 API、升级策略
  ├─ analytics/     趋势统计、聚合指标
  └─ audit/         操作审计日志

frontend/ (Vue3)
  ├─ src/api/       与后端模块一一对应的请求封装
  ├─ src/stores/    Pinia(用户/环境/客户/规则等)
  ├─ src/router/    路由(与 prototype 页面对应)
  ├─ src/layouts/   AppLayout(侧边栏+顶栏)、BlankLayout(登录/向导)
  ├─ src/views/     页面(见 §6 与 prototype 对照表)
  ├─ src/components/ 复用组件(空态/骨架/状态标签/严重级别标签/SQL 编辑器等)
  └─ src/styles/    主题(沿用 prototype/assets/style.css 的设计令牌)

要求：
- 业务库连接信息(口令)从环境变量/密钥管理注入，禁止入库明文；入库需加密。
- 提供 docker-compose（平台库 MySQL/PostgreSQL + Redis）与本地一键启动脚本。
- Flyway 管理平台库表结构（见 §3）。
```

---

## 3. 数据模型（平台库表结构提示词）

```
请用 Flyway 在"平台库"(与业务库隔离)创建下列表（字段名按需调整，保持语义一致）。所有表含 id、created_at、updated_at、created_by、updated_by、deleted(逻辑删除)。

# 客户与租户
customer(客户)
  - code(唯一), name, industry(行业), isolation_mode(DB_PER_CUSTOMER|TENANT_SHARED),
    product_line(产品线), deploy_version(当前部署版本,语义化), server_group(所在服务器/集群),
    accounting_period_type(自然月|445|项目期间制|自定义), current_period(如 2026-06),
    closing_rule(结账日/时点描述), sla_level(GOLD|SILVER|BRONZE), sla_availability,
    sla_check_freq(检测频率), sla_alert_sla(告警响应时长), wecom_group_ref(告警分流群),
    status(ACTIVE|DISABLED)
tenant(租户，仅 TENANT_SHARED 模式)
  - customer_id(所属租户群), tenant_key(=业务库 tenant_id 值), name, current_period, status
customer_ledger(多账簿) - customer_id, ledger_name, standard(中国准则|IFRS|税务|自定义), monitor_enabled
customer_currency(多币种) - customer_id, currency_code, is_base(是否本位币), fx_source(汇率来源)
customer_consolidation(合并口径) - customer_id, in_consolidation(bool), consolidation_level(合并层级),
    consolidation_entity(合并主体), elimination_rule_set_id(抵消规则集,可空表示待沉淀)

# 数据源
data_source - customer_id(或 tenant_pool 维度), db_type(MYSQL|ORACLE|SQLSERVER|DM|POSTGRES),
    jdbc_url, username, password_cipher(加密), mode(PRIMARY|READ_REPLICA),
    tenant_column(租户隔离字段名,默认 tenant_id), status(ONLINE|DELAY|FAILED), last_probe_at, latency_ms

# 规则与模板
rule(规则,当前生效版本视图) - rule_key(唯一), name, category, type(5 种机制之一), severity,
    spec_json(机制相关参数:sql/left_sql/right_sql/min/max/tolerance 等),
    scope_json(products[]/version_expr/servers[]/rule_sets[]/customers[]),
    invariant(bool,是否纳入基线回归), invariant_metric, invariant_tolerance,
    schedule_json(触发方式/cron/期间), alert_json(渠道/分流/@人),
    current_version, enabled(bool), source_template_id(可空,来源模板)
rule_version(规则历史版本) - rule_id, version, spec_json, scope_json, severity, change_summary, author, snapshot_json(完整快照,供回滚)
rule_set(规则集/行业集) - name, industry, product_line, version, builtin(bool)
rule_set_item - rule_set_id, rule_key, effective_version_expr
rule_template(模板库,与 rule_set 可合并管理) - 同 rule_set，含 status(PUBLISHED|DRAFT)
customer_rule(客户实际生效规则的覆盖/微调) - customer_id, rule_key, override_spec_json,
    is_local_modified(bool,本地是否微调过), enabled, effective(按版本判定后的实际生效)

# 调度与执行
detect_task(检测任务) - name, trigger_type(CRON|EVENT|CI_GATE|MANUAL), cron, scope_json,
    concurrency, enabled, last_run_at, next_run_at, status
detect_run(检测运行) - task_id(可空), customer_id/tenant_key, label(版本/触发标记),
    trigger_type, started_at, finished_at, duration_ms, total, passed, failed, errored, skipped,
    max_severity, gate_severity, gate_passed(bool)
check_result(单条检查结果) - run_id, rule_key, rule_name, category, severity,
    status(PASSED|FAILED|ERROR|SKIPPED), message, metrics_json, duration_ms, error
check_violation_sample(违规样本) - check_result_id, sample_json(脱敏后的违规行)

# 基线与回归
baseline(基线快照) - customer_id/tenant_key, period, related_version, status(AUDITED|PENDING),
    metrics_json(不变量 key->value), created_by
regression_finding(回归发现) - run_id, rule_key, metric, baseline_value, current_value, diff, tolerance, status(REGRESSION|OK)

# 灰度发布
rollout(灰度发布) - target_type(RULE|RULE_SET|TEMPLATE), target_ref, from_version, to_version,
    dimension(BY_SERVER|BY_CUSTOMER|BY_TENANT_PCT), strategy_json(批次方案/观察期/推进方式=MANUAL),
    rollback_json(误报率阈值/错误率阈值/动作), status(DRAFT|RUNNING|PAUSED|DONE|ROLLED_BACK), current_batch
rollout_batch(批次) - rollout_id, seq, scope_desc, scope_json, observe_minutes,
    status(PENDING|GRAYING|OBSERVING|PASSED|ROLLED_BACK), metrics_json(执行数/失败/误报率), advanced_by(人工确认人), advanced_at
template_sync(模板差异同步) - customer_id, template_id, from_version, to_version,
    diff_json(新增/修改/弃用/本地自定义/冲突), decisions_json(逐条:KEEP_LOCAL|ADOPT_TEMPLATE|SKIP), result

# 告警与审计
alert(告警) - run_id/check_result_id, customer_id/tenant_key, rule_key, severity, title, content_json,
    status(PENDING|PROCESSING|CLOSED), assignee, wecom_push_status(SENT|FAILED|SILENCED), created_at, closed_at
alert_channel(告警渠道) - type(WECOM_WEBHOOK|WECOM_APP), config_cipher, msg_type(MARKDOWN|TEXT|TEMPLATE_CARD),
    mention_users[], gate_severity, dedup_window, quiet_hours, escalation_json, scope(默认/客户/租户群)
wecom_push_log(推送记录) - alert_id, channel_id, payload_json, http_status, result, pushed_at
audit_log(审计) - actor, role, action(EDIT_RULE|APPLY_TEMPLATE|ROLLOUT|RESET_BASELINE|EDIT_DATASOURCE...),
    target, change_summary, ip, created_at  (只追加,不可改删)

# 权限
sys_user / sys_role / sys_user_role / role_permission；内置角色：ADMIN/RESEARCH(研发)/FINANCE(财务)/READONLY(只读)。

要求：为高频查询建索引(customer_id, rule_key, run_id, period, status, created_at)；金额字段 DECIMAL(20,4)；spec/scope/metrics 等用 JSON 列。
```

---

## 4. 通用约定（跨模块一致性）

```
- 统一返回体：ApiResult<T>{ code:0 成功/非0 业务错误, message, data, traceId }；异常用全局 @RestControllerAdvice 转换。
- 分页：PageResult<T>{ records, total, page, size }；列表接口统一支持 page/size/keyword/排序/筛选。
- 枚举集中定义并前后端共享：IsolationMode, DbType, RuleCategory, RuleType(5机制), Severity, CheckStatus, TriggerType, RolloutStatus, AlertStatus, SyncDecision。
- 金额：BigDecimal + DECIMAL(20,4)，比较一律 abs(a-b) <= tolerance(默认 0.005)。
- 版本表达式解析器：支持 ">=v3.0"、"<=v9.0"、"=v4.0"、"[v3.2,v9.0)"、"*/不限"；语义化版本比较；统一工具类 VersionSpec.matches(deployVersion)。
- 时间统一 UTC 存储，接口返回 ISO8601，前端按租户/用户时区展示。
- 所有写操作经审计切面记录 audit_log（注解 @Audited(action=...)）。
- 业务库访问统一经 ReadOnlyDataSourceRouter；执行前强制校验语句为只读(禁止非 SELECT)；超时与最大返回行数限制；样本抽样默认上限 20 行并脱敏。
```

---

## 5. 后端模块提示词（逐个开发）

> 每个模块：先读「§1 总纲 + §3 数据模型 + §4 约定」，再实现，最后用「§7 逻辑完整性检查清单」对应条目自检。

### 5.1 认证与权限（security）
```
实现：账号密码登录 + 企业微信扫码登录(OAuth/自建应用回调) + JWT(含刷新)；RBAC(ADMIN/RESEARCH/FINANCE/READONLY)，权限矩阵见原型 audit.html；接口级 @PreAuthorize；登录失败锁定、密码加密(BCrypt)。READONLY 不可写；ROLLOUT/RESET_BASELINE 等高危操作按角色与审批控制。
接口：POST /auth/login, /auth/wecom/callback, /auth/refresh, /auth/logout, GET /auth/me, /me/permissions。
```

### 5.2 动态多数据源与数据源管理（datasource）
```
实现：AbstractRoutingDataSource 按"客户/租户群"运行时路由；HikariCP 按客户隔离连接池与限流；只读校验(SQL 解析仅允许 SELECT/CTE)；连接探活定时任务(更新 status/latency)；只读副本优先；口令加密存取(KMS/AES)。
数据源 CRUD + 连接测试 + 批量探活。租户隔离模式记录 tenant_column。
接口：CRUD /datasources，POST /datasources/{id}/test，POST /datasources/probe。
```

### 5.3 客户与租户（tenant）
```
实现：客户/租户群 CRUD（两种隔离模式）；字段含行业、产品线、当前部署版本、所在服务器、会计期间(制度/当前期间/结账日/历史锁定)、SLA 等级与派生的检测频率与告警响应、多账簿、多币种(本位币+外币+汇率来源)、合并口径(是否纳入合并/层级/主体/抵消规则集可空)。
租户隔离模式：支持"自动发现租户"(扫描业务库 distinct tenant_id 落 tenant 表)。
客户详情聚合接口(健康度趋势、当前未通过、近期检测、账簿币种合并)。
接口：CRUD /customers, /customers/{id}/detail, /customers/{id}/tenants, POST /customers/{id}/discover-tenants, 子资源 ledgers/currencies/consolidation。
```

### 5.4 规则、规则集、模板、版本生效（rule）
```
实现：规则 CRUD(声明式 spec_json + scope_json)；规则集/行业模板；customer_rule 覆盖与本地微调标记；规则版本化(每次保存写 rule_version + 快照)，支持查看历史与"回滚到指定版本"(回滚=以旧快照生成新版本并审计)。
版本生效判定：给定客户 deploy_version，对每条规则用 VersionSpec + product_line + server_group 计算 effective；列表与详情返回 effective/skipped 原因(如"版本未达 v4.0")。
模板"一键套用"：选目标客户/租户群 → 按每条规则 effective_version_expr 与目标 deploy_version 取交集 → 仅下发达标规则，未达标保留但不执行；结果写 customer_rule 并审计；可对接灰度。
接口：CRUD /rules, GET /rules/{key}/versions, POST /rules/{key}/rollback, CRUD /rule-templates, POST /rule-templates/{id}/apply, GET /rules/effective?customerId=。
```

### 5.5 检查引擎（engine）
```
实现 5 种机制执行器(策略模式)：scalar_zero/scalar_equality/scalar_range/rows_empty/cross_system_scalar_equality。
- 参数绑定：自动注入 :period、:tenant_id(租户隔离)、客户上下文；金额容差比较。
- rows_empty：先 COUNT 再抽样(上限+脱敏)返回违规样本。
- 跨系统：同时取两个数据源标量比较。
- 异常隔离：单条规则执行异常→status=ERROR(不中断整体)；记录耗时。
租户隔离高效扫描：对同一规则用单条 GROUP BY tenant_id HAVING 聚合定位异常租户，再对异常租户下钻明细；避免逐租户 N 次查询。
执行入口：CheckEngine.run(customer/tenantScope, rules) -> DetectRun（含每条 CheckResult）。
```

### 5.6 调度（schedule）
```
实现：Quartz/XXL-Job 调度 detect_task；触发类型 CRON/EVENT(结账事件)/CI_GATE(发布前 Webhook)/MANUAL；独立库按客户并行、租户群聚合扫描；结账高峰错峰与并发上限；失败重试2次后转告警；记录执行历史与下次执行时间；分布式锁防重复执行。
接口：CRUD /tasks, POST /tasks/{id}/run, POST /tasks/{id}/pause|resume, POST /ci-gate/run(供流水线调用,返回门禁结果与退出码语义)。
```

### 5.7 检测结果（result）
```
实现：detect_run/check_result/violation_sample 落库与查询；运行详情(按状态/类别筛选、违规明细下钻、通过项折叠)；报表导出(JSON/Markdown/Excel)。门禁判定：max_severity >= gate 则 gate_passed=false。
接口：GET /runs, GET /runs/{id}, GET /runs/{id}/results?status=&category=, GET /runs/{id}/export。
```

### 5.8 基线与回归（baseline）
```
实现：从一次运行抽取 invariant 指标生成 baseline；审定流程(PENDING->AUDITED，仅 FINANCE/ADMIN)；回归对比：当前不变量指标 vs 已审定基线，abs(diff)>tolerance→REGRESSION；已结账期间锁定基线；"重置基线"需审定并审计。回归发现并入 detect_run 参与门禁。
接口：CRUD /baselines, POST /baselines/snapshot, POST /baselines/{id}/audit, POST /runs/{id}/regression?baselineId=。
```

### 5.9 灰度发布与模板差异同步（rollout）
```
灰度发布引擎：
- 维度 BY_SERVER/BY_CUSTOMER/BY_TENANT_PCT；批次方案(单服务器→单集群→全部集群→全量)；每批观察期(分钟)。
- 推进方式默认 MANUAL(人工确认)：观察达标后不自动放量，需有权限者确认推进；可选 AUTO。
- 自动回滚：批次观察期内误报率>阈值(默认2%)或执行错误率>阈值(默认5%)→回滚本批至上一版本并推送企业微信；记录 advanced_by/审计。
- 仅对"版本达标"的目标纳入批次。
模板差异同步：
- 计算 模板新版本 vs 客户本地 的差异：新增(+)/修改(~)/弃用(-)/本地自定义(保留)/冲突(本地微调过且模板也变)。
- 冲突项必须人工逐条决定 KEEP_LOCAL|ADOPT_TEMPLATE；普通修改可勾选；同步生成新版本，可接灰度。
接口：CRUD /rollouts, POST /rollouts/{id}/advance(人工确认推进), POST /rollouts/{id}/rollback, POST /rollouts/{id}/pause;
     GET /template-sync/diff?customerId=&templateId=, POST /template-sync/apply(含 decisions)。
```

### 5.10 告警与企业微信（alert）
```
实现：检查失败按级别聚合为 alert；推送策略(>=gate 才推、同规则 dedup 窗口合并、静默时段、升级策略：CRITICAL 超时未处理升级@主管)；按客户/租户群分流到不同企业微信群；@责任人。
企业微信：群机器人 Webhook 与 自建应用两种；消息类型 Markdown/Text/模板卡片；卡片字段(客户/租户、规则、差额、违规单据、影响、责任人、详情链接)，见原型 wecom.html；推送失败重试与 wecom_push_log。
告警处置闭环：认领/转交/标记闭环(PENDING->PROCESSING->CLOSED)；移动端 API。
接口：GET /alerts, POST /alerts/{id}/claim|transfer|close, CRUD /alert-channels, POST /alert-channels/{id}/test, GET /mobile/alerts。
```

### 5.11 趋势统计与审计（analytics / audit）
```
analytics：通过率趋势、失败按类别(时间序列)、MTTR、回归拦截数、Top 失败规则、行业健康度热力；聚合可预计算/缓存。接口 GET /analytics/*。
audit：审计切面写 audit_log(只追加)；查询/筛选；今日动作统计。接口 GET /audit-logs。
```

---

## 6. 前端模块提示词（页面与原型一一对应）

```
逐页还原 prototype/ 并接通后端接口，使用 Element Plus + Pinia + ECharts。设计令牌沿用 prototype/assets/style.css。

页面对照(prototype 文件 → Vue 视图)：
- login.html            → Login（账号密码 / 企业微信扫码 Tab）
- onboarding.html       → Onboarding（5 步向导：客户→数据源→规则集→告警→启动检测）
- index.html            → Dashboard（KPI、客户健康表、通过率环图、失败分类、最新告警）
- tenants.html          → CustomerList（两模式 Tab、行业/期间/SLA、行→详情）
- tenant-edit.html      → CustomerEdit（基本/数据源/会计期间/SLA/关联产品版本/账簿币种合并/规则集）
- customer-detail.html  → CustomerDetail（概览/规则/历史/数据源/基线 Tab）
- rules.html            → RuleList（类别 Tab、适用产品/生效版本列、启停、编辑/历史）
- rule-edit.html        → RuleEdit（基本/约束定义/适用范围与版本生效/调度/告警/YAML 预览/测试运行弹窗/迭代回归）
- rule-history.html     → RuleHistory（版本时间线 + 差异对比 + 回滚）
- rule-templates.html   → RuleTemplates（行业模板卡片 + 明细 + 一键套用/差异/灰度）
- template-diff.html    → TemplateDiff（差异清单 + 冲突逐条人工决定 + 并排对比）
- results.html          → RunDetail（概览、失败项下钻、违规样本、通过项折叠、趋势分析入口）
- analytics.html        → Analytics（ECharts：通过率折线/失败堆叠/Top规则/行业热力/MTTR）
- datasources.html      → DataSourceList（类型/连接/读写模式/状态/探活/测试）
- tasks.html            → TaskList（触发方式/周期/并发/执行历史）
- baselines.html        → BaselineList（快照/审定/回归对比明细）
- rollout.html          → Rollout（步骤条/批次进度/人工确认推进/回滚阈值）
- audit.html            → Audit（权限矩阵/成员/审计日志）
- alerts.html           → AlertCenter（级别筛选/处置状态/企业微信推送状态/移动端入口）
- wecom.html            → WecomSettings（接入/推送策略/分流/消息卡片预览）
- mobile-alerts.html    → MobileAlerts（移动端列表/详情，响应式或独立 H5）
- ui-states.html        → 复用组件参考：EmptyState/Skeleton/ErrorState/ResultToast

通用组件：SeverityTag、StatusTag、ModePill、SqlEditor(只读语法高亮)、VersionExprInput、
JsonViewer、PhonePreview(企业微信卡片)、ChartLine/ChartStackedBar/Heatmap、EmptyState/Skeleton/ErrorState。
交互要求：列表统一加载骨架与空态；表单校验与危险操作二次确认；权限不足按钮禁用并提示。
```

---

## 7. 逻辑完整性检查清单（防止漏逻辑 — 每模块自检）

> 让 AI 在每个模块完成后逐条核对，输出"满足/缺失"并补齐。这是保证"逻辑不缺失"的关键。

### 7.1 数据源与只读安全
- [ ] 仅允许 SELECT/CTE，拦截任何写语句（INSERT/UPDATE/DELETE/DDL/存储过程副作用）。
- [ ] 优先只读副本；连接池按客户隔离并限流；查询超时与最大返回行数限制。
- [ ] 口令加密存储，运行时解密；日志/接口不回显口令。
- [ ] 连接探活更新状态(ONLINE/DELAY/FAILED)；失败有重连与告警。
- [ ] 五种数据库(MySQL/Oracle/SQLServer/达梦/PG)方言差异(分页、字符串、日期)已处理。

### 7.2 多租户两模式
- [ ] 独立库模式按客户路由到独立 DataSource。
- [ ] 租户隔离模式所有查询自动注入 tenant 过滤；禁止跨租户串数据。
- [ ] 租户隔离高效扫描：单条聚合 SQL 定位异常租户 + 仅异常租户下钻（避免 N 次查询）。
- [ ] 自动发现租户：新增租户能被纳入；下线租户能停检。

### 7.3 规则引擎(5 机制)
- [ ] 每种机制都有执行器与单测；金额比较带容差；NULL 视为 0 的口径一致。
- [ ] rows_empty 先 COUNT 再抽样，样本上限+脱敏。
- [ ] 参数(:period/:tenant_id/客户上下文)正确绑定，防 SQL 注入(参数化)。
- [ ] 单条规则异常→ERROR 且不影响整体；记录耗时与错误。
- [ ] 跨系统机制能同时连两个数据源比较。

### 7.4 规则版本与产品版本挂钩
- [ ] 版本表达式解析覆盖 >=、<=、=、区间、不限；语义化版本比较正确(v9 > v10? 必须按数值段)。
- [ ] 生效判定 = 产品线匹配 ∧ 服务器匹配 ∧ 版本表达式匹配 ∧ enabled。
- [ ] 未达标规则"保留但跳过"，并给出 skipped 原因；客户升级后自动生效（无需人工重配）。
- [ ] 每次保存写 rule_version + 完整快照；回滚=以旧快照生成新版本(不物理回退)，并审计。

### 7.5 模板与一键套用 / 差异同步
- [ ] 套用按 版本交集 仅下发达标规则；写 customer_rule 并审计。
- [ ] 差异分类正确：新增/修改/弃用/本地自定义/冲突。
- [ ] 冲突(本地微调过且模板也变)必须人工逐条决定 KEEP_LOCAL|ADOPT_TEMPLATE，系统不自动覆盖。
- [ ] 同步生成新版本，可接灰度；本地自定义(模板无)默认保留。

### 7.6 调度与执行
- [ ] 四类触发(CRON/EVENT/CI_GATE/MANUAL)均可用；分布式锁防重复执行。
- [ ] 独立库并行 + 租户群聚合；结账高峰错峰与并发上限。
- [ ] 失败重试2次后转告警；执行历史与下次时间正确。
- [ ] CI 门禁接口返回门禁结论(gate_passed)与可供流水线判定的退出码语义。

### 7.7 检测结果与门禁
- [ ] 运行汇总统计(total/passed/failed/errored/skipped/max_severity)正确。
- [ ] 门禁：max_severity >= gate → gate_passed=false。
- [ ] 违规样本可下钻、脱敏；通过项可折叠；报表可导出。

### 7.8 基线与回归
- [ ] invariant 指标快照与对比正确；容差判定。
- [ ] 已审定基线才参与回归；已结账期间锁定；重置基线需审定+审计。
- [ ] 回归发现并入运行结果并参与门禁/告警。

### 7.9 灰度发布
- [ ] 三种维度 + 分批方案 + 每批观察期；默认人工确认推进(达标不自动放量)。
- [ ] 自动回滚：误报率/错误率超阈值→回滚本批+推送企业微信；记录确认人与审计。
- [ ] 仅版本达标目标纳入批次；暂停/继续/全量/回滚状态机完整。

### 7.10 告警与企业微信
- [ ] 仅 >=gate 推送；dedup 合并、静默时段、升级策略生效。
- [ ] 按客户/租户群分流到不同群；@责任人；卡片字段完整。
- [ ] 群机器人与自建应用两种；推送失败重试 + push_log；处置闭环(认领/转交/关闭)。
- [ ] 移动端列表/详情与 Web 数据一致。

### 7.11 权限与审计
- [ ] RBAC 四角色权限矩阵落地；READONLY 不可写；高危操作受控。
- [ ] 所有写操作写 audit_log(只追加，不可改删)；含 actor/action/target/变更摘要/IP。

### 7.12 通用/非功能
- [ ] 金额一律 BigDecimal/DECIMAL，禁止 double 比较。
- [ ] 统一返回体/异常/分页；接口 OpenAPI 文档；关键路径单测。
- [ ] 时间 UTC 存储本地化展示；i18n 预留；空态/加载/错误态统一(见 ui-states)。
- [ ] 平台库与业务库严格隔离；敏感配置走环境变量/密钥管理。

---

## 8. 关键算法/伪代码提示（消除歧义）

```
# 版本生效判定
effective(rule, customer) =
   rule.enabled
   && (rule.scope.products 为空 || contains(customer.product_line))
   && (rule.scope.servers 为空 || contains(customer.server_group))
   && VersionSpec(rule.scope.version_expr).matches(customer.deploy_version)
否则 skipped，reason = 不匹配的维度（如 "版本未达 v4.0"）。

# 租户隔离高效扫描(以借贷平衡为例)
1) 聚合：SELECT tenant_id, SUM(debit)-SUM(credit) d FROM gl_voucher_entry
        WHERE period=:p GROUP BY tenant_id HAVING ABS(d) > :tol  -> 异常租户集合
2) 仅对异常租户下钻明细(逐凭证)，生成违规样本；其余租户判定通过。

# 灰度批次推进(人工确认)
for batch in rollout.batches:
   下发 to_version 到 batch.scope(仅版本达标目标)
   进入 OBSERVING，累计 误报率/错误率
   if 误报率>阈值 or 错误率>阈值: 回滚本批到 from_version + 推送企业微信 + status=ROLLED_BACK; break
   if 观察期已满 且 指标达标: status=PASSED; 等待人工点击 advance 才进入下一批(默认 MANUAL)

# 回归对比
for key in baseline.metrics:  # 仅 invariant 指标，且 baseline 已审定
   cur = 当前运行该 key 的指标
   if abs(cur - base) > tolerance: 记 REGRESSION(并入运行结果/告警/门禁)
```

---

## 9. 建议开发顺序（里程碑）

```
M1 地基：工程骨架 + 平台库(Flyway) + 统一返回体/异常/枚举 + 认证与RBAC + 审计切面。
M2 数据底座：动态多数据源 + 只读安全 + 数据源管理 + 客户/租户(两模式)及子资源(账簿/币种/合并/会计期间/SLA)。
M3 规则核心：规则CRUD/版本化 + 5种检查引擎 + 版本生效判定 + 规则集/模板 + 一键套用。
M4 运行闭环：调度(CRON/事件/CI门禁/手动) + 检测执行 + 结果与违规下钻 + 门禁 + 报表导出。
M5 回归与告警：基线快照/审定/回归对比 + 告警聚合 + 企业微信推送 + 处置闭环 + 移动端。
M6 发布治理：灰度/分批发布引擎(人工确认+自动回滚) + 模板差异对比与选择性同步。
M7 可视化与体验：Dashboard + 趋势统计(ECharts) + 客户详情 + 规则历史回滚 + 空态/骨架/错误态打磨。
M8 加固：性能(聚合扫描/缓存)、并发与分布式锁、安全(脱敏/加密/限流)、E2E 与压测、文档。

每个里程碑结束：对照 §7 对应清单自检并补齐，再进入下一里程碑。
```

---

## 10. 给 Cursor 的单条"启动提示词"（可直接发）

```
请阅读仓库内 docs/AI开发提示词.md（开发提示词包）、docs/ARCHITECTURE.md（账务校验方法论）、prototype/（22 页高保真原型，从 login.html 进入）以及 erp_acc_monitor/（Python 校验引擎参考实现）。
我们要用 Java 17 + Spring Boot 3 + Vue3 + Element Plus 实现「普讯科技产品应用数据监测平台」。
请先按提示词包 §2 初始化前后端工程骨架与 §3 平台库 Flyway 迁移，然后从里程碑 M1 开始实现，并在每个里程碑结束时对照 §7 逻辑完整性检查清单自检、补齐缺失逻辑后再继续。开始前先给出你的实现计划与待我确认的关键取舍。
```


