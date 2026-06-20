"""演示数据生成器。

构建两套相互关联的 SQLite 库（财务系统 + 订单系统），用于离线演示监测服务。
`inject_error=True` 会模拟"一次研发迭代引入的账务错误"，让多类检查失败。
"""

from __future__ import annotations

import sqlite3
from pathlib import Path

PERIOD = "2026-05"

_FINANCE_SCHEMA = """
CREATE TABLE gl_voucher_entry (
    id INTEGER PRIMARY KEY,
    voucher_no TEXT NOT NULL,
    period TEXT NOT NULL,
    account_code TEXT NOT NULL,
    debit REAL NOT NULL DEFAULT 0,
    credit REAL NOT NULL DEFAULT 0
);
CREATE TABLE gl_account_balance (
    account_code TEXT NOT NULL,
    period TEXT NOT NULL,
    opening REAL NOT NULL DEFAULT 0,
    debit REAL NOT NULL DEFAULT 0,
    credit REAL NOT NULL DEFAULT 0,
    closing REAL NOT NULL DEFAULT 0
);
CREATE TABLE ar_detail (
    id INTEGER PRIMARY KEY,
    customer TEXT NOT NULL,
    period TEXT NOT NULL,
    amount REAL NOT NULL,
    account_code TEXT NOT NULL
);
CREATE TABLE ap_detail (
    id INTEGER PRIMARY KEY,
    supplier TEXT NOT NULL,
    period TEXT NOT NULL,
    amount REAL NOT NULL,
    account_code TEXT NOT NULL
);
CREATE TABLE inventory_txn (
    id INTEGER PRIMARY KEY,
    period TEXT NOT NULL,
    item TEXT NOT NULL,
    qty REAL NOT NULL,
    unit_cost REAL NOT NULL,
    amount REAL NOT NULL
);
"""

_ORDERS_SCHEMA = """
CREATE TABLE order_system_ar (
    id INTEGER PRIMARY KEY,
    customer TEXT NOT NULL,
    period TEXT NOT NULL,
    amount REAL NOT NULL
);
"""


def _reset(path: str | Path) -> sqlite3.Connection:
    p = Path(path)
    if p.exists():
        p.unlink()
    p.parent.mkdir(parents=True, exist_ok=True)
    return sqlite3.connect(str(p))


def build_demo(finance_path: str | Path, orders_path: str | Path, *, inject_error: bool = False) -> None:
    # ---- 财务系统 ----
    fin = _reset(finance_path)
    fin.executescript(_FINANCE_SCHEMA)

    # 凭证分录：每张凭证借贷各自平衡
    revenue_credit = 5200.0 if inject_error else 5000.0  # 注入错误：让凭证不平 + 收入不变量回归
    vouchers = [
        ("V20260501", "1122", 5000.0, 0.0),
        ("V20260501", "6001", 0.0, revenue_credit),
        ("V20260502", "1401", 3000.0, 0.0),
        ("V20260502", "1002", 0.0, 3000.0),
        ("V20260503", "5401", 2400.0, 0.0),
        ("V20260503", "1405", 0.0, 2400.0),
        ("V20260504", "2202", 0.0, 0.0),
    ]
    fin.executemany(
        "INSERT INTO gl_voucher_entry(voucher_no, period, account_code, debit, credit) "
        "VALUES (?, ?, ?, ?, ?)",
        [(v[0], PERIOD, v[1], v[2], v[3]) for v in vouchers],
    )

    # 科目余额：opening + debit - credit = closing
    ap_closing = 2100.0 if inject_error else 2000.0  # 注入错误：破坏连续性 + 应付勾稽
    # 采用带符号的滚存余额约定：closing = opening + debit - credit。
    # 收入/负债等贷方科目的余额表现为负数（如 6001 收入 closing = -5000）。
    balances = [
        ("1122", 0.0, 5000.0, 0.0, 5000.0),      # 应收（借方）：明细合计=5000
        ("2202", 2000.0, 0.0, 0.0, ap_closing),  # 应付（期初承前）：明细合计=2000
        ("6001", 0.0, 0.0, 5000.0, -5000.0),     # 主营收入（贷方）：滚存为 -5000
    ]
    fin.executemany(
        "INSERT INTO gl_account_balance(account_code, period, opening, debit, credit, closing) "
        "VALUES (?, ?, ?, ?, ?, ?)",
        [(b[0], PERIOD, b[1], b[2], b[3], b[4]) for b in balances],
    )

    # 应收明细：合计 5000
    fin.executemany(
        "INSERT INTO ar_detail(customer, period, amount, account_code) VALUES (?, ?, ?, ?)",
        [
            ("客户A", PERIOD, 1000.0, "1122"),
            ("客户B", PERIOD, 2500.0, "1122"),
            ("客户C", PERIOD, 1500.0, "1122"),
        ],
    )
    # 应付明细：合计 2000
    fin.executemany(
        "INSERT INTO ap_detail(supplier, period, amount, account_code) VALUES (?, ?, ?, ?)",
        [
            ("供应商X", PERIOD, 800.0, "2202"),
            ("供应商Y", PERIOD, 1200.0, "2202"),
        ],
    )
    # 存货：amount = qty * unit_cost
    item_b_amount = 1599.0 if inject_error else 1600.0  # 注入错误：金额≠数量×单价
    fin.executemany(
        "INSERT INTO inventory_txn(period, item, qty, unit_cost, amount) VALUES (?, ?, ?, ?, ?)",
        [
            (PERIOD, "物料A", 100.0, 12.5, 1250.0),
            (PERIOD, "物料B", 200.0, 8.0, item_b_amount),
            (PERIOD, "物料C", 50.0, 30.0, 1500.0),
        ],
    )
    fin.commit()
    fin.close()

    # ---- 订单系统 ----
    orders = _reset(orders_path)
    orders.executescript(_ORDERS_SCHEMA)
    # 订单系统应收合计应与财务一致 = 5000
    orders.executemany(
        "INSERT INTO order_system_ar(customer, period, amount) VALUES (?, ?, ?)",
        [
            ("客户A", PERIOD, 1000.0),
            ("客户B", PERIOD, 2500.0),
            ("客户C", PERIOD, 1500.0),
        ],
    )
    orders.commit()
    orders.close()
