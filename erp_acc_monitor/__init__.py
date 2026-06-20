"""ERP 账务正确性监测服务。

通过声明式约束检查 + 基线回归对比，在系统持续迭代的过程中守住账务数据的正确性。
"""

from .severity import Severity
from .models import CheckStatus, CheckResult, CheckRun

__version__ = "0.1.0"

__all__ = [
    "Severity",
    "CheckStatus",
    "CheckResult",
    "CheckRun",
    "__version__",
]
