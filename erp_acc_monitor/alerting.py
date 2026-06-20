"""告警接口。

仅依赖标准库（urllib），便于在任意环境运行。
可扩展为钉钉 / 企业微信 / 飞书 / 邮件等渠道。
"""

from __future__ import annotations

import json
import urllib.request
from abc import ABC, abstractmethod

from .models import CheckRun
from .severity import Severity


class Alerter(ABC):
    @abstractmethod
    def send(self, run: CheckRun, *, gate: Severity) -> None: ...


class ConsoleAlerter(Alerter):
    """把达到门禁级别的问题打印到控制台（默认告警渠道）。"""

    def send(self, run: CheckRun, *, gate: Severity) -> None:
        problems = [r for r in run.problems if r.severity >= gate]
        if not problems:
            return
        print(f"\n[告警] 检测到 {len(problems)} 个 >= {gate.label} 级别的账务问题：")
        for r in problems:
            print(f"  - [{r.severity.label}] {r.key} {r.name}: {r.message}")


class WebhookAlerter(Alerter):
    """把告警以 JSON POST 到指定 webhook（钉钉/企业微信/飞书自定义机器人等）。"""

    def __init__(self, url: str, *, timeout: float = 10.0):
        self.url = url
        self.timeout = timeout

    def build_payload(self, run: CheckRun, gate: Severity) -> dict:
        problems = [r for r in run.problems if r.severity >= gate]
        return {
            "run_id": run.run_id,
            "label": run.label,
            "gate": gate.label,
            "problem_count": len(problems),
            "summary": run.to_dict()["summary"],
            "problems": [
                {
                    "key": r.key,
                    "name": r.name,
                    "category": r.category,
                    "severity": r.severity.label,
                    "status": r.status.value,
                    "message": r.message,
                }
                for r in problems
            ],
        }

    def send(self, run: CheckRun, *, gate: Severity) -> None:
        payload = self.build_payload(run, gate)
        if payload["problem_count"] == 0:
            return
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        req = urllib.request.Request(
            self.url, data=data, headers={"Content-Type": "application/json"}, method="POST"
        )
        urllib.request.urlopen(req, timeout=self.timeout)  # noqa: S310 - 受信任的内部 webhook
