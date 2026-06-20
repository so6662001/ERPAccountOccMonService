"""跨系统一致性检查。

同一笔业务在不同系统里的口径必须一致，例如：
订单系统的应收总额 == 财务系统的应收总额。
需要同时连接两个不同的数据源做标量对比。
"""

from __future__ import annotations

from ..models import CheckStatus
from .base import Check, CheckContext
from .sql_checks import _to_float


class CrossSystemScalarEqualityCheck(Check):
    category = "cross_system"

    def __init__(
        self,
        *args,
        left_datasource: str,
        left_sql: str,
        right_datasource: str,
        right_sql: str,
        tolerance: float | None = None,
        **kwargs,
    ):
        super().__init__(*args, **kwargs)
        self.left_datasource = left_datasource
        self.left_sql = left_sql
        self.right_datasource = right_datasource
        self.right_sql = right_sql
        self.tolerance = tolerance

    def evaluate(self, ctx: CheckContext):
        tol = self.tolerance if self.tolerance is not None else ctx.default_tolerance
        left = _to_float(ctx.sources.get(self.left_datasource).scalar(self.left_sql))
        right = _to_float(ctx.sources.get(self.right_datasource).scalar(self.right_sql))
        diff = left - right
        metrics = {
            "left_datasource": self.left_datasource,
            "right_datasource": self.right_datasource,
            "left": left,
            "right": right,
            "diff": diff,
            "tolerance": tol,
        }
        if abs(diff) <= tol:
            return (
                CheckStatus.PASSED,
                f"跨系统一致：{self.left_datasource}={left:.4f} ≈ {self.right_datasource}={right:.4f}",
                metrics,
                [],
            )
        return (
            CheckStatus.FAILED,
            (
                f"跨系统不一致：{self.left_datasource}={left:.4f}, "
                f"{self.right_datasource}={right:.4f}, 差额={diff:.4f}（容差 ±{tol}）"
            ),
            metrics,
            [],
        )
