"""基线快照与迭代回归对比。

迭代回归防护的核心：把"业务上不该变"的关键指标固化为基线，
新一次运行后与基线逐项对比，偏离超过容差即判定为"回归"。
"""

from __future__ import annotations

import datetime as _dt
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from .checks.base import Check
from .models import CheckResult, CheckRun, CheckStatus
from .severity import Severity

#: 各检查类型默认纳入对比的指标名（未显式指定 invariant_metric 时使用）
_DEFAULT_METRIC_BY_KEYSET = ("value", "left", "diff", "violations")


def _pick_metric_name(metrics: dict[str, Any], explicit: str | None) -> str | None:
    if explicit:
        return explicit if explicit in metrics else None
    for name in _DEFAULT_METRIC_BY_KEYSET:
        if name in metrics:
            return name
    return None


@dataclass
class Baseline:
    """一份基线：检查 key -> {指标名: 数值}。"""

    metrics: dict[str, dict[str, float]]
    created_at: str
    run_id: str = ""
    label: str = ""

    @classmethod
    def from_run(cls, run: CheckRun, checks: list[Check]) -> "Baseline":
        invariant_keys = {c.key for c in checks if c.invariant}
        explicit = {c.key: c.invariant_metric for c in checks}
        metrics: dict[str, dict[str, float]] = {}
        for r in run.results:
            if r.key not in invariant_keys:
                continue
            mname = _pick_metric_name(r.metrics, explicit.get(r.key))
            if mname is None:
                continue
            metrics[r.key] = {mname: float(r.metrics[mname])}
        return cls(
            metrics=metrics,
            created_at=_dt.datetime.now(_dt.timezone.utc).isoformat(),
            run_id=run.run_id,
            label=run.label,
        )

    def to_dict(self) -> dict[str, Any]:
        return {
            "created_at": self.created_at,
            "run_id": self.run_id,
            "label": self.label,
            "metrics": self.metrics,
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "Baseline":
        return cls(
            metrics=data.get("metrics", {}),
            created_at=data.get("created_at", ""),
            run_id=data.get("run_id", ""),
            label=data.get("label", ""),
        )

    def save(self, path: str | Path) -> None:
        p = Path(path)
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(json.dumps(self.to_dict(), ensure_ascii=False, indent=2), encoding="utf-8")

    @classmethod
    def load(cls, path: str | Path) -> "Baseline":
        data = json.loads(Path(path).read_text(encoding="utf-8"))
        return cls.from_dict(data)


def compare_to_baseline(
    run: CheckRun,
    baseline: Baseline,
    checks: list[Check],
    *,
    default_tolerance: float = 0.005,
    severity: Severity | str = Severity.HIGH,
) -> list[CheckResult]:
    """把当前运行的不变量指标与基线对比，产出"回归"类检查结果。

    返回的结果可直接追加进 CheckRun，参与门禁判定。
    """
    sev = Severity.parse(severity)
    check_by_key = {c.key: c for c in checks}
    results: list[CheckResult] = []

    current_by_key = {r.key: r for r in run.results}

    for key, base_metrics in baseline.metrics.items():
        chk = check_by_key.get(key)
        tol = default_tolerance
        if chk is not None and chk.invariant_tolerance is not None:
            tol = chk.invariant_tolerance
        cur = current_by_key.get(key)
        reg_key = f"regression::{key}"
        name = f"回归对比：{(chk.name if chk else key)}"

        if cur is None:
            results.append(
                CheckResult(
                    key=reg_key, name=name, category="regression", severity=sev,
                    status=CheckStatus.ERROR,
                    message=f"基线含指标但当前运行缺少检查 '{key}'，无法对比",
                )
            )
            continue

        for mname, base_value in base_metrics.items():
            cur_value = cur.metrics.get(mname)
            metrics = {"baseline": base_value, "current": cur_value, "tolerance": tol, "metric": mname}
            if cur_value is None:
                results.append(
                    CheckResult(
                        key=reg_key, name=name, category="regression", severity=sev,
                        status=CheckStatus.ERROR, metrics=metrics,
                        message=f"当前运行缺少指标 '{mname}'，无法对比",
                    )
                )
                continue
            diff = float(cur_value) - float(base_value)
            metrics["diff"] = diff
            if abs(diff) <= tol:
                status = CheckStatus.PASSED
                msg = f"指标 '{mname}' 与基线一致：{cur_value} ≈ {base_value}"
            else:
                status = CheckStatus.FAILED
                msg = (
                    f"检测到回归：指标 '{mname}' 由基线 {base_value} 变为 {cur_value}"
                    f"（差额 {diff}，容差 ±{tol}）—— 该指标不应随迭代变化"
                )
            results.append(
                CheckResult(
                    key=reg_key, name=name, category="regression",
                    severity=sev, status=status, metrics=metrics, message=msg,
                )
            )
    return results
