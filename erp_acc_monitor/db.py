"""数据访问层。

对多套系统的数据库做统一的**只读**访问，按名引用。
基于 SQLAlchemy，可对接 MySQL / PostgreSQL / Oracle / SQL Server / 达梦 等。
"""

from __future__ import annotations

from typing import Any

from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine


class DataSource:
    """单个数据源的只读封装。"""

    def __init__(self, name: str, url: str, *, connect_args: dict | None = None):
        self.name = name
        self.url = url
        # future=True 使用 SQLAlchemy 2.0 风格；pool_pre_ping 防止长连接失效。
        self._engine: Engine = create_engine(
            url,
            future=True,
            pool_pre_ping=True,
            connect_args=connect_args or {},
        )

    def scalar(self, sql: str, params: dict | None = None) -> Any:
        """执行查询并返回首行首列。结果为空时返回 None。"""
        with self._engine.connect() as conn:
            return conn.execute(text(sql), params or {}).scalar()

    def rows(self, sql: str, params: dict | None = None, limit: int | None = None) -> list[dict[str, Any]]:
        """执行查询并以字典列表返回。limit 仅用于截断返回给上层的样本数量。"""
        with self._engine.connect() as conn:
            result = conn.execute(text(sql), params or {})
            mappings = result.mappings()
            out: list[dict[str, Any]] = []
            for i, row in enumerate(mappings):
                if limit is not None and i >= limit:
                    break
                out.append(dict(row))
            return out

    def count(self, sql: str, params: dict | None = None) -> int:
        """把传入的 SQL 当作子查询统计行数。"""
        wrapped = f"SELECT COUNT(*) FROM ({sql}) AS _sub"
        value = self.scalar(wrapped, params)
        return int(value or 0)

    def dispose(self) -> None:
        self._engine.dispose()


class DataSourceManager:
    """按名管理多个数据源；跨系统一致性检查需要同时访问多个源。"""

    def __init__(self) -> None:
        self._sources: dict[str, DataSource] = {}

    def add(self, source: DataSource) -> None:
        self._sources[source.name] = source

    def register(self, name: str, url: str, *, connect_args: dict | None = None) -> DataSource:
        ds = DataSource(name, url, connect_args=connect_args)
        self.add(ds)
        return ds

    def get(self, name: str) -> DataSource:
        if name not in self._sources:
            available = ", ".join(self._sources) or "（无）"
            raise KeyError(f"未找到数据源 '{name}'，已注册：{available}")
        return self._sources[name]

    @property
    def names(self) -> list[str]:
        return list(self._sources)

    def dispose_all(self) -> None:
        for ds in self._sources.values():
            ds.dispose()
