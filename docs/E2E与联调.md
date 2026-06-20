# 端到端测试与本地联调

## 1. 端到端集成测试（Testcontainers）

`backend/src/test/java/com/puxun/monitor/e2e/PlatformE2ETest.java`：启动**真实 MySQL 容器** → 执行 **Flyway 全量迁移(V1–V7)** → 加载 **Spring 全上下文** → 验证登录 / 客户与规则 CRUD / 规则版本递增与历史。

- 依赖 Docker。**有 Docker 的环境会真实运行**；无 Docker 时由 `@Testcontainers(disabledWithoutDocker = true)` 自动跳过（不会导致构建失败）。
- 运行：
  ```bash
  cd backend && mvn test          # 有 Docker 时 E2E 实跑；无则跳过
  ```
- CI：`.github/workflows/ci.yml` 的 `backend` job 在 GitHub runner（自带 Docker）上运行 `mvn test`，**Testcontainers E2E 将真实执行**。

## 2a. 一键全栈运行态联调（根目录 docker compose，推荐）

在**带 Docker 的环境**（如在 Cursor 网页端配置了 Docker 的 Cloud Agent 环境）一条命令拉起 MySQL + Redis + 后端 + 前端：

```bash
docker compose up -d --build
#   前端:        http://localhost:8088   （登录 admin / admin123）
#   后端 Swagger: http://localhost:8080/api/swagger-ui.html
```

- 后端容器启动时自动等待 MySQL 健康 → 执行 Flyway 全量迁移(V1–V7) → 创建管理员 → 启动调度。
- 前端 Nginx 将 `/api` 反代到后端容器（服务名 `backend:8080`）。
- 停止：`docker compose down`（保留数据卷）；清库：`docker compose down -v`。

> 镜像：`backend/Dockerfile`（Maven 多阶段 → JRE）、`frontend/Dockerfile`（Node 构建 → Nginx + `nginx.conf` 反代）。

## 2b. 本地分别启动（docker compose 仅起依赖）

```bash
# 1) 起平台库 MySQL + Redis
cd backend && docker compose up -d

# 2) 启后端（首启自动 Flyway 迁移 + 创建管理员 admin/admin123）
export ADMIN_INIT_PWD=admin123
mvn spring-boot:run
#   Swagger: http://localhost:8080/api/swagger-ui.html

# 3) 启前端（已配 /api 代理到 8080）
cd ../frontend && npm install && npm run dev
#   访问 http://localhost:5173 ，用 admin/admin123 登录

# 4) 接入一套业务库做真实检测（只读账号），在「数据源」「客户与租户」「检查规则」中配置，
#    然后在「检测任务」手动执行或定时触发 → 「检测结果」查看 → 失败自动推送企业微信
```

## 3. CI 流水线

`.github/workflows/ci.yml` 三个并行 job：
- **backend**：JDK 17 + `mvn test`（含真实 MySQL 容器 E2E）
- **frontend**：Node 20 + `npm ci` + `npm run test`(Vitest) + `npm run build`
- **engine**：Python 3.12 + `pytest`

> 也可将后端 CI 门禁接口 `POST /api/ci-gate/run` 接入业务系统的发布流水线：发布前对目标客户跑账务回归检测，`exitCode=1` 即阻断发布。
