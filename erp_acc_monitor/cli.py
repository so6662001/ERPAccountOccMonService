"""命令行入口。

子命令：
  init-demo      生成 SQLite 演示数据（可 --inject-error 模拟迭代引入的账错）
  list-checks    列出配置中的全部检查
  run            执行检查并输出报告（可结合基线做回归对比），按门禁返回退出码
  baseline-save  执行检查并把不变量指标固化为基线
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from . import __version__
from .alerting import ConsoleAlerter, WebhookAlerter
from .baseline import Baseline, compare_to_baseline
from .config import AppConfig
from .demo import build_demo
from .engine import Engine
from .reporting import render
from .severity import Severity

_DEFAULT_CONFIG = "config/monitor.example.yaml"


def _build_engine(cfg: AppConfig) -> Engine:
    return Engine(
        cfg.sources,
        cfg.checks,
        default_tolerance=cfg.settings.default_tolerance,
        sample_limit=cfg.settings.sample_limit,
    )


def _cmd_init_demo(args: argparse.Namespace) -> int:
    build_demo(args.finance, args.orders, inject_error=args.inject_error)
    state = "（已注入账务错误，用于演示失败场景）" if args.inject_error else ""
    print(f"已生成演示数据：{args.finance} / {args.orders} {state}")
    print(f"接下来可运行：  python -m erp_acc_monitor run --config {_DEFAULT_CONFIG}")
    return 0


def _cmd_list_checks(args: argparse.Namespace) -> int:
    cfg = AppConfig.load(args.config)
    print(f"共 {len(cfg.checks)} 条检查：")
    for c in cfg.checks:
        inv = "  [不变量]" if c.invariant else ""
        print(f"  - {c.key:<32} {c.severity.label:<8} {c.category:<14} {c.name}{inv}")
    return 0


def _cmd_run(args: argparse.Namespace) -> int:
    cfg = AppConfig.load(args.config)
    engine = _build_engine(cfg)
    keys = args.keys.split(",") if args.keys else None
    categories = args.categories.split(",") if args.categories else None
    run = engine.run(keys=keys, categories=categories, label=args.label or "")

    gate = Severity.parse(args.gate) if args.gate else cfg.settings.gate_severity

    # 迭代回归对比
    if args.regression:
        baseline_path = args.baseline or cfg.settings.baseline_path
        if not Path(baseline_path).exists():
            print(f"[警告] 未找到基线文件 {baseline_path}，跳过回归对比。"
                  f"请先执行 baseline-save 生成基线。", file=sys.stderr)
        else:
            baseline = Baseline.load(baseline_path)
            reg_results = compare_to_baseline(
                run, baseline, cfg.checks,
                default_tolerance=cfg.settings.default_tolerance,
                severity=args.regression_severity,
            )
            for r in reg_results:
                run.add(r)

    print(render(run, args.format))

    if args.output:
        Path(args.output).write_text(render(run, args.output_format or args.format), encoding="utf-8")
        print(f"\n报告已写入：{args.output}", file=sys.stderr)

    # 告警
    if args.alert:
        ConsoleAlerter().send(run, gate=gate)
        if cfg.settings.webhook_url:
            try:
                WebhookAlerter(cfg.settings.webhook_url).send(run, gate=gate)
            except Exception as exc:  # noqa: BLE001
                print(f"[警告] webhook 告警发送失败：{exc}", file=sys.stderr)

    cfg.sources.dispose_all()
    return 0 if run.is_ok(gate) else 1


def _cmd_baseline_save(args: argparse.Namespace) -> int:
    cfg = AppConfig.load(args.config)
    engine = _build_engine(cfg)
    run = engine.run(label="baseline")
    baseline = Baseline.from_run(run, cfg.checks)
    out = args.output or cfg.settings.baseline_path
    baseline.save(out)
    print(f"已保存基线到 {out}，包含 {len(baseline.metrics)} 个不变量指标：")
    for key, metrics in baseline.metrics.items():
        print(f"  - {key}: {metrics}")
    cfg.sources.dispose_all()
    return 0


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="erp_acc_monitor",
        description="ERP 账务正确性监测服务",
    )
    p.add_argument("--version", action="version", version=f"%(prog)s {__version__}")
    sub = p.add_subparsers(dest="command", required=True)

    pd = sub.add_parser("init-demo", help="生成 SQLite 演示数据")
    pd.add_argument("--finance", default="demo_finance.sqlite")
    pd.add_argument("--orders", default="demo_orders.sqlite")
    pd.add_argument("--inject-error", action="store_true", help="注入账务错误以演示失败场景")
    pd.set_defaults(func=_cmd_init_demo)

    pl = sub.add_parser("list-checks", help="列出全部检查")
    pl.add_argument("--config", "-c", default=_DEFAULT_CONFIG)
    pl.set_defaults(func=_cmd_list_checks)

    pr = sub.add_parser("run", help="执行检查并输出报告")
    pr.add_argument("--config", "-c", default=_DEFAULT_CONFIG)
    pr.add_argument("--format", "-f", default="console", choices=["console", "json", "markdown"])
    pr.add_argument("--categories", help="只跑指定类别，逗号分隔")
    pr.add_argument("--keys", help="只跑指定 key，逗号分隔")
    pr.add_argument("--label", help="本次运行的标签（如版本号）")
    pr.add_argument("--gate", help="门禁级别，覆盖配置（INFO/LOW/MEDIUM/HIGH/CRITICAL）")
    pr.add_argument("--regression", action="store_true", help="与基线对比，检测迭代回归")
    pr.add_argument("--baseline", help="基线文件路径，覆盖配置")
    pr.add_argument("--regression-severity", default="HIGH", help="回归问题的严重级别")
    pr.add_argument("--output", "-o", help="把报告写入文件")
    pr.add_argument("--output-format", help="文件报告格式，默认同 --format")
    pr.add_argument("--alert", action="store_true", help="对达到门禁级别的问题触发告警")
    pr.set_defaults(func=_cmd_run)

    pb = sub.add_parser("baseline-save", help="把不变量指标固化为基线")
    pb.add_argument("--config", "-c", default=_DEFAULT_CONFIG)
    pb.add_argument("--output", "-o", help="基线输出路径，覆盖配置")
    pb.set_defaults(func=_cmd_baseline_save)

    return p


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
