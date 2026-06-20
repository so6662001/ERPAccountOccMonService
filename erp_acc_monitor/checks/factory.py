"""从配置（dict / YAML 节点）构建检查实例。"""

from __future__ import annotations

from typing import Any

from .base import Check
from .cross_system import CrossSystemScalarEqualityCheck
from .sql_checks import (
    RowsEmptyCheck,
    ScalarEqualityCheck,
    ScalarRangeCheck,
    ScalarZeroCheck,
)

#: type 名称 -> 检查类
CHECK_TYPES: dict[str, type[Check]] = {
    "scalar_zero": ScalarZeroCheck,
    "scalar_equality": ScalarEqualityCheck,
    "scalar_range": ScalarRangeCheck,
    "rows_empty": RowsEmptyCheck,
    "cross_system_scalar_equality": CrossSystemScalarEqualityCheck,
}

#: 各检查类型专属参数（其余通用参数由基类处理）
_TYPE_PARAMS: dict[str, tuple[str, ...]] = {
    "scalar_zero": ("datasource", "sql", "tolerance"),
    "scalar_equality": ("datasource", "left_sql", "right_sql", "tolerance"),
    "scalar_range": ("datasource", "sql", "min_value", "max_value"),
    "rows_empty": ("datasource", "sql"),
    "cross_system_scalar_equality": (
        "left_datasource",
        "left_sql",
        "right_datasource",
        "right_sql",
        "tolerance",
    ),
}

_COMMON_PARAMS = (
    "severity",
    "category",
    "description",
    "invariant",
    "invariant_metric",
    "invariant_tolerance",
)


def build_check(config: dict[str, Any]) -> Check:
    cfg = dict(config)
    try:
        ctype = cfg.pop("type")
    except KeyError as exc:
        raise ValueError(f"检查配置缺少 'type' 字段：{config!r}") from exc
    if ctype not in CHECK_TYPES:
        valid = ", ".join(sorted(CHECK_TYPES))
        raise ValueError(f"未知的检查类型 '{ctype}'，可选：{valid}")

    key = cfg.pop("key", None)
    name = cfg.pop("name", key)
    if not key:
        raise ValueError(f"检查配置缺少 'key' 字段：{config!r}")

    kwargs: dict[str, Any] = {}
    for param in _COMMON_PARAMS:
        if param in cfg:
            kwargs[param] = cfg.pop(param)
    for param in _TYPE_PARAMS[ctype]:
        if param in cfg:
            kwargs[param] = cfg.pop(param)

    if cfg:
        unknown = ", ".join(sorted(cfg))
        raise ValueError(f"检查 '{key}' 含有未识别的配置项：{unknown}")

    cls = CHECK_TYPES[ctype]
    return cls(key, name, **kwargs)


def build_checks(configs: list[dict[str, Any]]) -> list[Check]:
    checks = [build_check(c) for c in configs]
    keys = [c.key for c in checks]
    dupes = {k for k in keys if keys.count(k) > 1}
    if dupes:
        raise ValueError(f"存在重复的检查 key：{', '.join(sorted(dupes))}")
    return checks
