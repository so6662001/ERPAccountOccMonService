"""声明式 SQL 检查。

这几种机制可以覆盖绝大多数账务约束：
- ScalarZeroCheck      : 某标量必须为 0（如 全局 Σ借 − Σ贷 = 0）
- ScalarEqualityCheck  : 两个标量必须相等（如 应收明细合计 = 应收总账余额）
- ScalarRangeCheck     : 某标量必须落在区间（如 单位成本 >= 0）
- RowsEmptyCheck       : 违规明细查询必须返回 0 行（如 逐凭证借贷不平 / 孤儿单据）
"""

from __future__ import annotations

from typing import Any

from ..models import CheckStatus
from .base import Check, CheckContext


def _to_float(value: Any) -> float:
    """把数据库返回值规整成 float；NULL/None 视为 0。"""
    if value is None:
        return 0.0
    return float(value)


class _SqlCheck(Check):
    def __init__(self, *args, datasource: str, **kwargs):
        super().__init__(*args, **kwargs)
        self.datasource = datasource


class ScalarZeroCheck(_SqlCheck):
    """SQL 返回的标量必须约等于 0（容差内）。"""

    category = "balance"

    def __init__(self, *args, sql: str, tolerance: float | None = None, **kwargs):
        super().__init__(*args, **kwargs)
        self.sql = sql
        self.tolerance = tolerance

    def evaluate(self, ctx: CheckContext):
        tol = self.tolerance if self.tolerance is not None else ctx.default_tolerance
        value = _to_float(ctx.sources.get(self.datasource).scalar(self.sql))
        metrics = {"value": value, "tolerance": tol}
        if abs(value) <= tol:
            return CheckStatus.PASSED, f"标量值 {value:.4f} 在容差 ±{tol} 内", metrics, []
        return (
            CheckStatus.FAILED,
            f"期望约为 0，实际为 {value:.4f}（容差 ±{tol}）",
            metrics,
            [],
        )


class ScalarEqualityCheck(_SqlCheck):
    """两段 SQL 返回的标量必须相等（容差内）。"""

    category = "reconciliation"

    def __init__(self, *args, left_sql: str, right_sql: str, tolerance: float | None = None, **kwargs):
        super().__init__(*args, **kwargs)
        self.left_sql = left_sql
        self.right_sql = right_sql
        self.tolerance = tolerance

    def evaluate(self, ctx: CheckContext):
        tol = self.tolerance if self.tolerance is not None else ctx.default_tolerance
        ds = ctx.sources.get(self.datasource)
        left = _to_float(ds.scalar(self.left_sql))
        right = _to_float(ds.scalar(self.right_sql))
        diff = left - right
        metrics = {"left": left, "right": right, "diff": diff, "tolerance": tol}
        if abs(diff) <= tol:
            return CheckStatus.PASSED, f"两侧相等：{left:.4f} ≈ {right:.4f}", metrics, []
        return (
            CheckStatus.FAILED,
            f"两侧不一致：left={left:.4f}, right={right:.4f}, 差额={diff:.4f}（容差 ±{tol}）",
            metrics,
            [],
        )


class ScalarRangeCheck(_SqlCheck):
    """SQL 返回的标量必须落在 [min_value, max_value] 区间内。"""

    category = "cost"

    def __init__(
        self,
        *args,
        sql: str,
        min_value: float | None = None,
        max_value: float | None = None,
        **kwargs,
    ):
        super().__init__(*args, **kwargs)
        self.sql = sql
        self.min_value = min_value
        self.max_value = max_value

    def evaluate(self, ctx: CheckContext):
        value = _to_float(ctx.sources.get(self.datasource).scalar(self.sql))
        metrics = {"value": value, "min": self.min_value, "max": self.max_value}
        if self.min_value is not None and value < self.min_value:
            return CheckStatus.FAILED, f"值 {value:.4f} 低于下限 {self.min_value}", metrics, []
        if self.max_value is not None and value > self.max_value:
            return CheckStatus.FAILED, f"值 {value:.4f} 超过上限 {self.max_value}", metrics, []
        return CheckStatus.PASSED, f"值 {value:.4f} 在允许区间内", metrics, []


class RowsEmptyCheck(_SqlCheck):
    """违规明细查询必须返回 0 行；否则抽样展示违规记录便于定位。"""

    category = "integrity"

    def __init__(self, *args, sql: str, **kwargs):
        super().__init__(*args, **kwargs)
        self.sql = sql

    def evaluate(self, ctx: CheckContext):
        ds = ctx.sources.get(self.datasource)
        # 用 COUNT 拿到总违规数，再单独抽样，避免一次性拉回海量数据。
        violations = ds.count(self.sql)
        metrics = {"violations": violations}
        if violations == 0:
            return CheckStatus.PASSED, "无违规记录", metrics, []
        samples = ds.rows(self.sql, limit=ctx.sample_limit)
        return (
            CheckStatus.FAILED,
            f"发现 {violations} 条违规记录（展示前 {len(samples)} 条）",
            metrics,
            samples,
        )
