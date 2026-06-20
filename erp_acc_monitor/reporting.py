"""报告输出：console / json / markdown。"""

from __future__ import annotations

import json

from .models import CheckRun, CheckStatus

_STATUS_MARK = {
    CheckStatus.PASSED: "✓",
    CheckStatus.FAILED: "✗",
    CheckStatus.ERROR: "!",
    CheckStatus.SKIPPED: "-",
}


def console_report(run: CheckRun, *, verbose: bool = False) -> str:
    lines: list[str] = []
    lines.append("=" * 70)
    lines.append(f"ERP 账务正确性监测报告  run_id={run.run_id}" + (f"  [{run.label}]" if run.label else ""))
    lines.append(f"开始：{run.started_at.isoformat()}  耗时：{run.duration_ms:.0f}ms")
    lines.append("=" * 70)

    for r in sorted(run.results, key=lambda x: (x.status == CheckStatus.PASSED, x.category, x.key)):
        mark = _STATUS_MARK.get(r.status, "?")
        lines.append(
            f"[{mark}] {r.status.value:<7} {r.severity.label:<8} {r.category:<14} {r.key}"
        )
        lines.append(f"        {r.name} — {r.message}")
        if verbose and r.metrics:
            lines.append(f"        指标: {json.dumps(r.metrics, ensure_ascii=False)}")
        if r.samples:
            for s in r.samples[:5]:
                lines.append(f"        · {json.dumps(s, ensure_ascii=False, default=str)}")

    lines.append("-" * 70)
    lines.append(
        f"合计 {run.total}  通过 {run.passed}  失败 {run.failed}  "
        f"错误 {run.errored}  跳过 {run.skipped}"
    )
    top = run.max_problem_severity()
    lines.append(f"最高问题级别：{top.label if top else '无'}")
    lines.append("=" * 70)
    return "\n".join(lines)


def json_report(run: CheckRun) -> str:
    return json.dumps(run.to_dict(), ensure_ascii=False, indent=2, default=str)


def markdown_report(run: CheckRun) -> str:
    lines: list[str] = []
    title = f"# ERP 账务正确性监测报告"
    if run.label:
        title += f"（{run.label}）"
    lines.append(title)
    lines.append("")
    lines.append(f"- run_id: `{run.run_id}`")
    lines.append(f"- 开始时间: {run.started_at.isoformat()}")
    lines.append(f"- 耗时: {run.duration_ms:.0f} ms")
    lines.append(
        f"- 概览: 合计 **{run.total}**，通过 **{run.passed}**，"
        f"失败 **{run.failed}**，错误 **{run.errored}**，跳过 **{run.skipped}**"
    )
    top = run.max_problem_severity()
    lines.append(f"- 最高问题级别: **{top.label if top else '无'}**")
    lines.append("")
    lines.append("| 状态 | 级别 | 类别 | Key | 名称 | 说明 |")
    lines.append("|---|---|---|---|---|---|")
    for r in sorted(run.results, key=lambda x: (x.status == CheckStatus.PASSED, x.category, x.key)):
        mark = _STATUS_MARK.get(r.status, "?")
        msg = r.message.replace("|", "\\|")
        lines.append(
            f"| {mark} {r.status.value} | {r.severity.label} | {r.category} | "
            f"`{r.key}` | {r.name} | {msg} |"
        )
    return "\n".join(lines)


RENDERERS = {
    "console": lambda run: console_report(run, verbose=True),
    "json": json_report,
    "markdown": markdown_report,
}


def render(run: CheckRun, fmt: str) -> str:
    if fmt not in RENDERERS:
        raise ValueError(f"未知的报告格式 '{fmt}'，可选：{', '.join(RENDERERS)}")
    return RENDERERS[fmt](run)
