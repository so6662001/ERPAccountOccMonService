"""ERP 账务正确性监测服务的单元/集成测试。"""

from __future__ import annotations

from pathlib import Path

import pytest

from erp_acc_monitor.baseline import Baseline, compare_to_baseline
from erp_acc_monitor.checks import build_check, build_checks
from erp_acc_monitor.config import AppConfig
from erp_acc_monitor.demo import build_demo
from erp_acc_monitor.engine import Engine
from erp_acc_monitor.models import CheckStatus
from erp_acc_monitor.severity import Severity


@pytest.fixture
def demo_dbs(tmp_path: Path, request):
    inject = getattr(request, "param", False)
    fin = tmp_path / "finance.sqlite"
    orders = tmp_path / "orders.sqlite"
    build_demo(fin, orders, inject_error=inject)
    return fin, orders


def _config_dict(fin: Path, orders: Path) -> dict:
    return {
        "settings": {"gate_severity": "HIGH", "baseline_path": "baseline.json"},
        "datasources": {
            "finance": {"url": f"sqlite:///{fin}"},
            "orders": {"url": f"sqlite:///{orders}"},
        },
        "checks": [
            {
                "key": "balance.global", "type": "scalar_zero", "name": "全局平衡",
                "category": "balance", "severity": "CRITICAL", "datasource": "finance",
                "sql": "SELECT SUM(debit)-SUM(credit) FROM gl_voucher_entry WHERE period='2026-05'",
            },
            {
                "key": "recon.ar", "type": "scalar_equality", "name": "应收勾稽",
                "severity": "CRITICAL", "datasource": "finance",
                "left_sql": "SELECT SUM(amount) FROM ar_detail WHERE period='2026-05'",
                "right_sql": "SELECT closing FROM gl_account_balance WHERE account_code='1122' AND period='2026-05'",
            },
            {
                "key": "cost.amount", "type": "rows_empty", "name": "成本金额",
                "category": "cost", "severity": "HIGH", "datasource": "finance",
                "sql": "SELECT id FROM inventory_txn WHERE period='2026-05' AND ABS(amount-qty*unit_cost)>0.005",
            },
            {
                "key": "xsys.ar", "type": "cross_system_scalar_equality", "name": "跨系统应收",
                "severity": "CRITICAL",
                "left_datasource": "finance",
                "left_sql": "SELECT SUM(amount) FROM ar_detail WHERE period='2026-05'",
                "right_datasource": "orders",
                "right_sql": "SELECT SUM(amount) FROM order_system_ar WHERE period='2026-05'",
            },
            {
                "key": "inv.revenue", "type": "scalar_range", "name": "收入不变量",
                "category": "invariant", "severity": "INFO", "datasource": "finance",
                "sql": "SELECT SUM(credit) FROM gl_voucher_entry WHERE account_code='6001' AND period='2026-05'",
                "invariant": True, "invariant_metric": "value",
            },
        ],
    }


def _engine(fin: Path, orders: Path) -> tuple[Engine, AppConfig]:
    cfg = AppConfig.from_dict(_config_dict(fin, orders))
    return Engine(cfg.sources, cfg.checks), cfg


# ----------------- 配置 / 工厂 -----------------

def test_build_check_requires_key():
    with pytest.raises(ValueError):
        build_check({"type": "scalar_zero", "datasource": "x", "sql": "SELECT 0"})


def test_build_check_unknown_type():
    with pytest.raises(ValueError):
        build_check({"type": "nope", "key": "k", "datasource": "x", "sql": "SELECT 0"})


def test_build_check_rejects_unknown_param():
    with pytest.raises(ValueError):
        build_check({"type": "scalar_zero", "key": "k", "datasource": "x", "sql": "SELECT 0", "bogus": 1})


def test_duplicate_keys_rejected():
    cfgs = [
        {"type": "scalar_zero", "key": "dup", "datasource": "x", "sql": "SELECT 0"},
        {"type": "scalar_zero", "key": "dup", "datasource": "x", "sql": "SELECT 0"},
    ]
    with pytest.raises(ValueError):
        build_checks(cfgs)


def test_env_expansion(monkeypatch):
    monkeypatch.setenv("DB_PWD", "s3cret")
    cfg = AppConfig.from_dict({
        "datasources": {"finance": {"url": "sqlite:///x.sqlite?pwd=${DB_PWD}"}},
        "checks": [],
    })
    assert "s3cret" in cfg.sources.get("finance").url


def test_env_expansion_missing_raises():
    with pytest.raises(ValueError):
        AppConfig.from_dict({
            "datasources": {"finance": "sqlite:///${NOT_SET_VAR_XYZ}"},
            "checks": [],
        })


# ----------------- 正常场景：全部通过 -----------------

@pytest.mark.parametrize("demo_dbs", [False], indirect=True)
def test_healthy_all_pass(demo_dbs):
    fin, orders = demo_dbs
    engine, cfg = _engine(fin, orders)
    run = engine.run()
    assert run.total == 5
    assert run.failed == 0 and run.errored == 0
    assert run.is_ok(Severity.HIGH)


# ----------------- 错误场景：多类检查失败 -----------------

@pytest.mark.parametrize("demo_dbs", [True], indirect=True)
def test_injected_errors_detected(demo_dbs):
    fin, orders = demo_dbs
    engine, cfg = _engine(fin, orders)
    run = engine.run()
    by_key = {r.key: r for r in run.results}
    assert by_key["balance.global"].status == CheckStatus.FAILED
    assert by_key["cost.amount"].status == CheckStatus.FAILED
    # 应收 / 跨系统未被注入错误，应当通过
    assert by_key["recon.ar"].status == CheckStatus.PASSED
    assert by_key["xsys.ar"].status == CheckStatus.PASSED
    assert not run.is_ok(Severity.HIGH)
    assert run.max_problem_severity() == Severity.CRITICAL


@pytest.mark.parametrize("demo_dbs", [True], indirect=True)
def test_rows_empty_returns_samples(demo_dbs):
    fin, orders = demo_dbs
    engine, _ = _engine(fin, orders)
    run = engine.run(keys=["cost.amount"])
    res = run.results[0]
    assert res.status == CheckStatus.FAILED
    assert res.metrics["violations"] == 1
    assert len(res.samples) == 1


# ----------------- 引擎选择 -----------------

@pytest.mark.parametrize("demo_dbs", [False], indirect=True)
def test_engine_filter_by_category(demo_dbs):
    fin, orders = demo_dbs
    engine, _ = _engine(fin, orders)
    run = engine.run(categories=["cost"])
    assert run.total == 1 and run.results[0].key == "cost.amount"


# ----------------- 异常隔离：坏 SQL 变成 ERROR 而非崩溃 -----------------

@pytest.mark.parametrize("demo_dbs", [False], indirect=True)
def test_bad_sql_isolated_as_error(demo_dbs):
    fin, orders = demo_dbs
    cfg = AppConfig.from_dict({
        "datasources": {"finance": {"url": f"sqlite:///{fin}"}},
        "checks": [
            {"type": "scalar_zero", "key": "bad", "datasource": "finance",
             "sql": "SELECT * FROM no_such_table"},
            {"type": "scalar_zero", "key": "good", "datasource": "finance",
             "sql": "SELECT 0"},
        ],
    })
    run = Engine(cfg.sources, cfg.checks).run()
    by_key = {r.key: r for r in run.results}
    assert by_key["bad"].status == CheckStatus.ERROR
    assert by_key["good"].status == CheckStatus.PASSED


# ----------------- 基线回归 -----------------

@pytest.mark.parametrize("demo_dbs", [False], indirect=True)
def test_baseline_save_and_no_regression(demo_dbs, tmp_path):
    fin, orders = demo_dbs
    engine, cfg = _engine(fin, orders)
    run = engine.run()
    baseline = Baseline.from_run(run, cfg.checks)
    assert "inv.revenue" in baseline.metrics
    bpath = tmp_path / "baseline.json"
    baseline.save(bpath)

    run2 = engine.run()
    reg = compare_to_baseline(run2, Baseline.load(bpath), cfg.checks)
    assert reg and all(r.status == CheckStatus.PASSED for r in reg)


def test_baseline_detects_regression(tmp_path):
    fin = tmp_path / "finance.sqlite"
    orders = tmp_path / "orders.sqlite"
    build_demo(fin, orders, inject_error=False)
    engine, cfg = _engine(fin, orders)
    baseline = Baseline.from_run(engine.run(), cfg.checks)

    # 模拟一次迭代后重建为"有错"的数据
    build_demo(fin, orders, inject_error=True)
    engine2, cfg2 = _engine(fin, orders)
    run2 = engine2.run()
    reg = compare_to_baseline(run2, baseline, cfg2.checks)
    assert any(r.status == CheckStatus.FAILED for r in reg)
    failed = [r for r in reg if r.status == CheckStatus.FAILED][0]
    assert failed.category == "regression"
    assert failed.metrics["baseline"] == 5000.0
    assert failed.metrics["current"] == 5200.0


# ----------------- severity -----------------

def test_severity_parse():
    assert Severity.parse("critical") == Severity.CRITICAL
    assert Severity.parse(Severity.LOW) == Severity.LOW
    assert Severity.parse(40) == Severity.HIGH
    with pytest.raises(ValueError):
        Severity.parse("nonsense")
