"""内置检查类型与配置工厂。"""

from .base import Check, CheckContext
from .sql_checks import (
    ScalarZeroCheck,
    ScalarEqualityCheck,
    ScalarRangeCheck,
    RowsEmptyCheck,
)
from .cross_system import CrossSystemScalarEqualityCheck
from .factory import build_check, build_checks, CHECK_TYPES

__all__ = [
    "Check",
    "CheckContext",
    "ScalarZeroCheck",
    "ScalarEqualityCheck",
    "ScalarRangeCheck",
    "RowsEmptyCheck",
    "CrossSystemScalarEqualityCheck",
    "build_check",
    "build_checks",
    "CHECK_TYPES",
]
