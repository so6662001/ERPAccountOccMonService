"""配置加载：从 YAML 构建数据源、检查与运行参数。

支持用 ``${ENV_VAR}`` 引用环境变量（数据库口令等敏感信息不应写进配置文件）。
"""

from __future__ import annotations

import os
import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml

from .checks.base import Check
from .checks.factory import build_checks
from .db import DataSourceManager
from .severity import Severity

_ENV_PATTERN = re.compile(r"\$\{([A-Za-z_][A-Za-z0-9_]*)\}")


def _expand_env(value: Any) -> Any:
    """递归地把字符串中的 ${VAR} 替换成环境变量值。"""
    if isinstance(value, str):
        def repl(m: re.Match) -> str:
            name = m.group(1)
            if name not in os.environ:
                raise ValueError(f"配置引用了未设置的环境变量：{name}")
            return os.environ[name]
        return _ENV_PATTERN.sub(repl, value)
    if isinstance(value, dict):
        return {k: _expand_env(v) for k, v in value.items()}
    if isinstance(value, list):
        return [_expand_env(v) for v in value]
    return value


@dataclass
class Settings:
    default_tolerance: float = 0.005
    sample_limit: int = 20
    gate_severity: Severity = Severity.HIGH
    baseline_path: str = "baseline.json"
    webhook_url: str | None = None


@dataclass
class AppConfig:
    settings: Settings
    sources: DataSourceManager
    checks: list[Check] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "AppConfig":
        data = _expand_env(data or {})

        raw_settings = data.get("settings", {}) or {}
        alert = raw_settings.get("alert", {}) or {}
        settings = Settings(
            default_tolerance=float(raw_settings.get("default_tolerance", 0.005)),
            sample_limit=int(raw_settings.get("sample_limit", 20)),
            gate_severity=Severity.parse(raw_settings.get("gate_severity", "HIGH")),
            baseline_path=str(raw_settings.get("baseline_path", "baseline.json")),
            webhook_url=alert.get("webhook_url") or None,
        )

        sources = DataSourceManager()
        for name, spec in (data.get("datasources", {}) or {}).items():
            if isinstance(spec, str):
                url = spec
                connect_args = None
            else:
                url = spec["url"]
                connect_args = spec.get("connect_args")
            sources.register(name, url, connect_args=connect_args)

        checks = build_checks(data.get("checks", []) or [])
        return cls(settings=settings, sources=sources, checks=checks)

    @classmethod
    def load(cls, path: str | Path) -> "AppConfig":
        data = yaml.safe_load(Path(path).read_text(encoding="utf-8"))
        return cls.from_dict(data)
