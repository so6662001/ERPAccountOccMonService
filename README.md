# ERPAccountOccMonService · ERP 账务正确性监测服务

> 在系统持续迭代的前提下，**守住账务数据正确性这条底线**——把"迭代引入的账务错误"（应收应付勾稽错、成本/利润失真、跨系统口径打架等）在进入生产、造成经济损失之前拦截下来。

## 它解决什么问题

很多业务系统底层都依赖 ERP 数据。每次研发迭代都可能在不经意间破坏账务的正确性，而账错往往直接等于钱：

- 应收/应付勾稽错 → 漏收欠款、坏账损失
- 成本核算错 → 利润失真 → 利润分配出错
- 跨系统金额不一致 → 对账对不上

本服务把"账务必须永远成立的恒等式"形式化为**可执行的约束检查**，并在两个关键时机持续验证：

1. **迭代回归防护（CI 门禁）**：发布前在冻结数据集上对比关键账务指标与基线，发现"本不该变却变了"，`critical` 不通过即阻断发布。
2. **生产持续监测**：结账节点后定时全量巡检，按严重级别告警。

> 设计与方法论详见 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)。

## 核心能力

- **声明式约束**：90% 的账务约束用 YAML 即可表达，无需写代码。
- **多检查机制**：标量为零 / 标量相等 / 标量区间 / 违规行集为空 / 跨数据源标量相等。
- **覆盖账务全维度**：平衡、勾稽、连续性、成本、完整性、跨系统、迭代回归。
- **多数据源**：基于 SQLAlchemy，支持 MySQL / PostgreSQL / Oracle / SQL Server / 达梦等，跨系统检查可同时连多个库。
- **基线回归**：把关键不变量指标固化为基线，迭代后自动对比，检测回归。
- **违规可定位**：失败检查会抽样展示具体违规单据，便于排查。
- **CI 友好**：按门禁级别返回退出码；可接入 webhook（钉钉/企业微信/飞书）告警。
- **全程只读**：监测服务不参与任何账务写入，绝不成为新的风险源。

## 安装

```bash
pip install -r requirements.txt
# 或开发安装： pip install -e .[test]
```

## 快速体验（内置 SQLite 演示，无需真实数据库）

```bash
# 1) 生成两套相互关联的演示库（财务系统 + 订单系统）
python -m erp_acc_monitor init-demo

# 2) 把"正确"状态下的关键指标固化为基线
python -m erp_acc_monitor baseline-save

# 3) 正常运行——全部通过，退出码 0
python -m erp_acc_monitor run

# 4) 模拟"一次迭代引入了账务错误"，并与基线做回归对比
python -m erp_acc_monitor init-demo --inject-error
python -m erp_acc_monitor run --regression --alert --label "v2.4-rollout"
#   → 借贷不平、应付勾稽错、连续性断裂、成本金额错、收入指标回归 全部被抓出，退出码 1
```

## 接入你自己的 ERP

1. 复制并修改配置：参考 [`config/monitor.example.yaml`](config/monitor.example.yaml)。
2. 配置只读数据源（口令用 `${ENV_VAR}` 从环境变量注入，不要写进文件）：

```yaml
datasources:
  finance:
    url: "mysql+pymysql://readonly:${DB_PWD}@10.0.0.1:3306/erp"
  orders:
    url: "postgresql+psycopg://ro:${ORD_PWD}@10.0.0.2:5432/orders"
```

3. 把财务/业务专家口中"这两个数必须相等""这张表不该有这种记录"逐条翻译成 YAML 约束（这是最有价值的资产）。
4. 挂上流水线：

```bash
# 发布前门禁：达到门禁级别即非零退出，阻断合并/发布
python -m erp_acc_monitor run -c config/monitor.yaml --regression --gate CRITICAL

# 生产定时巡检 + 告警
python -m erp_acc_monitor run -c config/monitor.yaml --alert -f json -o report.json
```

## 命令一览

| 命令 | 作用 |
|---|---|
| `init-demo [--inject-error]` | 生成 SQLite 演示数据 |
| `list-checks` | 列出配置中的全部检查 |
| `run` | 执行检查并输出报告（`--regression` 做回归对比，`--alert` 触发告警），按门禁返回退出码 |
| `baseline-save` | 把不变量指标固化为基线 |

`run` 常用参数：`--format console|json|markdown`、`--categories`、`--keys`、`--gate`、`--baseline`、`--output`、`--label`。

## 支持的检查类型

| type | 含义 | 典型用途 |
|---|---|---|
| `scalar_zero` | 标量须约等于 0 | 全局 Σ借 − Σ贷 = 0 |
| `scalar_equality` | 两标量须相等 | 应收明细合计 = 应收总账余额 |
| `scalar_range` | 标量须落在区间 | 单位成本 ≥ 0；也用作不变量指标载体 |
| `rows_empty` | 违规明细须为 0 行 | 逐凭证不平、连续性断裂、孤儿单据 |
| `cross_system_scalar_equality` | 跨数据源标量须相等 | 订单系统应收 = 财务系统应收 |

复杂场景可继承 `erp_acc_monitor.checks.base.Check` 编写 Python 插件检查。

## 测试

```bash
python -m pytest
```

## 目录结构

```
erp_acc_monitor/
  severity.py        严重级别（兼作 CI 门禁阈值）
  models.py          CheckResult / CheckRun 数据模型
  db.py              只读数据访问层（多数据源）
  checks/            检查基类、声明式 SQL 检查、跨系统检查、配置工厂
  engine.py          检查引擎（编排执行 + 异常隔离）
  baseline.py        基线快照与迭代回归对比
  reporting.py       console / json / markdown 报告
  alerting.py        控制台 / webhook 告警
  config.py          YAML 配置加载（支持 ${ENV} 注入）
  demo.py            内置演示数据生成器
  cli.py             命令行入口
config/monitor.example.yaml   示例配置（覆盖全部约束类别）
docs/ARCHITECTURE.md          架构与方法论
tests/                        测试
```
