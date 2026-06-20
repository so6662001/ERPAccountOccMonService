"""检查严重级别。

级别既用于告警分流，也用于 CI 门禁：达到门禁级别且未通过即判定整体失败。
"""

from __future__ import annotations

from enum import IntEnum


class Severity(IntEnum):
    """数值越大越严重，便于按"门禁级别"做比较。"""

    INFO = 10
    LOW = 20
    MEDIUM = 30
    HIGH = 40
    CRITICAL = 50

    @classmethod
    def parse(cls, value: "Severity | str | int") -> "Severity":
        if isinstance(value, Severity):
            return value
        if isinstance(value, int):
            return cls(value)
        key = str(value).strip().upper()
        try:
            return cls[key]
        except KeyError as exc:  # pragma: no cover - 配置错误提示
            valid = ", ".join(s.name for s in cls)
            raise ValueError(f"未知的严重级别 '{value}'，可选值：{valid}") from exc

    @property
    def label(self) -> str:
        return self.name
