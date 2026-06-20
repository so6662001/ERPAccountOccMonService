"""检查引擎：编排执行全部检查，汇总成一次 CheckRun。"""

from __future__ import annotations

from collections.abc import Iterable

from .checks.base import Check, CheckContext
from .db import DataSourceManager
from .models import CheckRun


class Engine:
    def __init__(
        self,
        sources: DataSourceManager,
        checks: list[Check],
        *,
        default_tolerance: float = 0.005,
        sample_limit: int = 20,
    ):
        self.sources = sources
        self.checks = checks
        self.ctx = CheckContext(
            sources=sources,
            default_tolerance=default_tolerance,
            sample_limit=sample_limit,
        )

    def select(
        self,
        keys: Iterable[str] | None = None,
        categories: Iterable[str] | None = None,
    ) -> list[Check]:
        selected = self.checks
        if keys:
            keyset = set(keys)
            selected = [c for c in selected if c.key in keyset]
        if categories:
            catset = set(categories)
            selected = [c for c in selected if c.category in catset]
        return selected

    def run(
        self,
        keys: Iterable[str] | None = None,
        categories: Iterable[str] | None = None,
        *,
        label: str = "",
    ) -> CheckRun:
        run = CheckRun(label=label)
        for check in self.select(keys, categories):
            run.add(check.run(self.ctx))
        return run.finish()
