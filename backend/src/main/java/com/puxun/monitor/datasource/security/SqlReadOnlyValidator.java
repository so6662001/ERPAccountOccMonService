package com.puxun.monitor.datasource.security;

import com.puxun.monitor.common.BizException;
import com.puxun.monitor.common.ResultCode;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.Select;

/**
 * 只读 SQL 校验：仅允许单条 SELECT/CTE，拦截写操作与多语句注入。
 * 这是平台对业务库"绝不写入"的强约束之一。
 */
public final class SqlReadOnlyValidator {

    private SqlReadOnlyValidator() {}

    public static void assertSelectOnly(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new BizException(ResultCode.READONLY_VIOLATION, "SQL 不能为空");
        }
        try {
            Statements stmts = CCJSqlParserUtil.parseStatements(sql);
            if (stmts.getStatements().size() != 1) {
                throw new BizException(ResultCode.READONLY_VIOLATION, "仅允许单条语句，禁止多语句");
            }
            Statement stmt = stmts.getStatements().get(0);
            if (!(stmt instanceof Select)) {
                throw new BizException(ResultCode.READONLY_VIOLATION,
                        "仅允许 SELECT 查询，禁止写操作/DDL: " + stmt.getClass().getSimpleName());
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // 解析失败：保守拒绝，避免绕过校验
            throw new BizException(ResultCode.READONLY_VIOLATION, "SQL 解析失败或不被允许: " + e.getMessage());
        }
    }
}
