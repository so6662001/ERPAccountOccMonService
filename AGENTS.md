# AGENTS.md

## Cursor Cloud specific instructions

This repo is a 3-component monorepo for an ERP accounting-correctness monitoring platform:

- **Python engine** (repo root: `erp_acc_monitor/`, `tests/`) — the flagship product: declarative accounting constraint checks + baseline regression. No external services needed.
- **Java backend** (`backend/`) — Spring Boot 3.2 + MyBatis-Plus + Flyway + Redis; needs MySQL + Redis to run.
- **Vue 3 frontend** (`frontend/`) — Vite dev server; proxies `/api` → `http://localhost:8080`.

Standard build/test/run commands are documented in `README.md`, `backend/README.md`, `docs/E2E与联调.md`, and `.github/workflows/ci.yml`. Notes below are only the non-obvious caveats for this VM.

### Services start each session (no systemd here — start manually)

The update script installs dependencies but does NOT start services. MySQL and Redis do not auto-start (no systemd/`docker`). Start them with the SysV scripts:

```bash
sudo service redis-server start
sudo service mysql start
```

On first-ever setup the MySQL `root` user was set to password `root` and database `puxun_monitor` was created (to match the backend defaults). These persist in the VM snapshot; if a fresh DB ever lacks them, run:

```bash
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'root'; CREATE DATABASE IF NOT EXISTS puxun_monitor CHARACTER SET utf8mb4;"
```

Run the services (dev mode):

```bash
# backend (first boot runs Flyway V1–V7 + creates admin/admin123)
cd backend && ADMIN_INIT_PWD=admin123 mvn spring-boot:run   # http://localhost:8080/api/swagger-ui.html
# frontend
npm --prefix frontend run dev                                # http://localhost:5173  (login admin/admin123)
```

### Non-obvious caveats

- **Python**: packages are installed system-wide via `pip --break-system-packages` (PEP 668). Use `python3` (no `python` alias) and invoke tools as modules: `python3 -m pytest`, `python3 -m erp_acc_monitor ...`. The `pytest`/`erp-acc-monitor` console scripts land in `~/.local/bin`, which is not on `PATH`.
- **Java**: JDK **21** is installed; the project targets Java 17 but builds and runs fine on 21. Maven runs on 21.
- **Docker is not installed.** Consequences: backend `mvn test` runs 37 tests with **2 skipped** — the Testcontainers `PlatformE2ETest` auto-skips without Docker (expected, not a failure). The root `docker compose up` full-stack path is unavailable; use the native local-dev workflow above instead.
- **Known backend boot blocker (code bug, unrelated to env):** `mvn spring-boot:run` connects to MySQL and applies all Flyway migrations, then fails with `AnalyticsMapper` bean-not-found. Cause: `@MapperScan("com.puxun.monitor.**.mapper")` in `MonitorApplication` does not cover `com.puxun.monitor.analytics.AnalyticsMapper` (it is not in a `.mapper` subpackage). This is never caught by `mvn test` because the only full-context test (`PlatformE2ETest`) is Docker-gated and skips here. Fix in code (move the interface to a `.mapper` package or broaden the scan) before the web platform can boot.
