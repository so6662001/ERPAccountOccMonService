"""检查结果的数据模型。"""

from __future__ import annotations

import datetime as _dt
import uuid
from dataclasses import dataclass, field
from enum import Enum
from typing import Any

from .severity import Severity


class CheckStatus(str, Enum):
    PASSED = "PASSED"
    FAILED = "FAILED"
    ERROR = "ERROR"  # 检查自身执行异常（如 SQL 报错），区别于"账务不通过"
    SKIPPED = "SKIPPED"

    @property
    def is_problem(self) -> bool:
        return self in (CheckStatus.FAILED, CheckStatus.ERROR)


def _now() -> _dt.datetime:
    return _dt.datetime.now(_dt.timezone.utc)


@dataclass
class CheckResult:
    """单条检查的结果。"""

    key: str
    name: str
    category: str
    severity: Severity
    status: CheckStatus
    message: str = ""
    metrics: dict[str, Any] = field(default_factory=dict)
    samples: list[dict[str, Any]] = field(default_factory=list)
    duration_ms: float = 0.0
    error: str | None = None

    @property
    def passed(self) -> bool:
        return self.status == CheckStatus.PASSED

    def to_dict(self) -> dict[str, Any]:
        return {
            "key": self.key,
            "name": self.name,
            "category": self.category,
            "severity": self.severity.label,
            "status": self.status.value,
            "message": self.message,
            "metrics": self.metrics,
            "samples": self.samples,
            "duration_ms": round(self.duration_ms, 2),
            "error": self.error,
        }


@dataclass
class CheckRun:
    """一次完整运行（包含全部检查结果）。"""

    results: list[CheckResult] = field(default_factory=list)
    run_id: str = field(default_factory=lambda: uuid.uuid4().hex[:12])
    started_at: _dt.datetime = field(default_factory=_now)
    finished_at: _dt.datetime | None = None
    label: str = ""

    def add(self, result: CheckResult) -> None:
        self.results.append(result)

    def finish(self) -> "CheckRun":
        self.finished_at = _now()
        return self

    @property
    def total(self) -> int:
        return len(self.results)

    def count(self, status: CheckStatus) -> int:
        return sum(1 for r in self.results if r.status == status)

    @property
    def passed(self) -> int:
        return self.count(CheckStatus.PASSED)

    @property
    def failed(self) -> int:
        return self.count(CheckStatus.FAILED)

    @property
    def errored(self) -> int:
        return self.count(CheckStatus.ERROR)

    @property
    def skipped(self) -> int:
        return self.count(CheckStatus.SKIPPED)

    @property
    def problems(self) -> list[CheckResult]:
        return [r for r in self.results if r.status.is_problem]

    def max_problem_severity(self) -> Severity | None:
        """所有未通过检查中的最高严重级别（用于门禁判定）。"""
        sev = [r.severity for r in self.results if r.status.is_problem]
        return max(sev) if sev else None

    def is_ok(self, gate: Severity) -> bool:
        """是否通过门禁：不存在 >= gate 级别的问题。"""
        top = self.max_problem_severity()
        return top is None or top < gate

    @property
    def duration_ms(self) -> float:
        if self.finished_at is None:
            return 0.0
        return (self.finished_at - self.started_at).total_seconds() * 1000.0

    def to_dict(self) -> dict[str, Any]:
        return {
            "run_id": self.run_id,
            "label": self.label,
            "started_at": self.started_at.isoformat(),
            "finished_at": self.finished_at.isoformat() if self.finished_at else None,
            "duration_ms": round(self.duration_ms, 2),
            "summary": {
                "total": self.total,
                "passed": self.passed,
                "failed": self.failed,
                "errored": self.errored,
                "skipped": self.skipped,
            },
            "results": [r.to_dict() for r in self.results],
        }
