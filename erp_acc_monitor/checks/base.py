"""检查基类与执行上下文。"""

from __future__ import annotations

import time
from abc import ABC, abstractmethod
from dataclasses import dataclass

from ..db import DataSourceManager
from ..models import CheckResult, CheckStatus
from ..severity import Severity


@dataclass
class CheckContext:
    """执行检查时的上下文：提供数据源访问与默认参数。"""

    sources: DataSourceManager
    default_tolerance: float = 0.005
    sample_limit: int = 20


class Check(ABC):
    """所有检查的基类。

    子类只需实现 ``evaluate``，返回 (status, message, metrics, samples)。
    计时、异常隔离由基类的 ``run`` 统一处理，单条检查报错不会中断整体运行。
    """

    #: 该检查所属类别（balance/reconciliation/continuity/cost/integrity/cross_system/regression…）
    category: str = "general"

    def __init__(
        self,
        key: str,
        name: str,
        *,
        severity: Severity | str = Severity.HIGH,
        category: str | None = None,
        description: str = "",
        invariant: bool = False,
        invariant_metric: str | None = None,
        invariant_tolerance: float | None = None,
    ):
        self.key = key
        self.name = name
        self.severity = Severity.parse(severity)
        if category is not None:
            self.category = category
        self.description = description
        # 回归防护：标记为 invariant 的检查，其指标会被纳入基线对比。
        self.invariant = invariant
        self.invariant_metric = invariant_metric
        self.invariant_tolerance = invariant_tolerance

    @abstractmethod
    def evaluate(self, ctx: CheckContext) -> tuple[CheckStatus, str, dict, list]:
        """返回 (status, message, metrics, samples)。"""

    def run(self, ctx: CheckContext) -> CheckResult:
        start = time.perf_counter()
        try:
            status, message, metrics, samples = self.evaluate(ctx)
            error = None
        except Exception as exc:  # noqa: BLE001 - 故意兜底，保证整体不中断
            status = CheckStatus.ERROR
            message = f"检查执行异常：{exc}"
            metrics, samples = {}, []
            error = f"{type(exc).__name__}: {exc}"
        duration_ms = (time.perf_counter() - start) * 1000.0
        return CheckResult(
            key=self.key,
            name=self.name,
            category=self.category,
            severity=self.severity,
            status=status,
            message=message,
            metrics=metrics,
            samples=samples,
            duration_ms=duration_ms,
            error=error,
        )
