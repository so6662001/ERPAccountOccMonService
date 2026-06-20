package com.puxun.monitor.datasource.runtime;

import com.puxun.monitor.datasource.security.SampleMasker;
import com.puxun.monitor.datasource.security.SqlReadOnlyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 业务库只读访问门面：所有出口先做只读校验，再走对应数据源的连接池。
 * 检查引擎只通过本门面访问业务库。
 */
@Component
@RequiredArgsConstructor
public class DataSourceFacade {

    private final BusinessDataSourceManager manager;

    private static final int DEFAULT_QUERY_TIMEOUT_SEC = 30;
    private static final int DEFAULT_MAX_ROWS = 10_000;

    private NamedParameterJdbcTemplate jdbc(Long dataSourceId) {
        var tpl = new NamedParameterJdbcTemplate(manager.resolve(dataSourceId));
        tpl.getJdbcTemplate().setQueryTimeout(DEFAULT_QUERY_TIMEOUT_SEC);
        tpl.getJdbcTemplate().setMaxRows(DEFAULT_MAX_ROWS);
        return tpl;
    }

    /** 标量查询：首行首列；空结果返回 null。 */
    public BigDecimal scalar(Long dataSourceId, String sql, Map<String, Object> params) {
        SqlReadOnlyValidator.assertSelectOnly(sql);
        List<BigDecimal> list = jdbc(dataSourceId).query(sql, params, (rs, n) -> {
            Object v = rs.getObject(1);
            return v == null ? null : new BigDecimal(v.toString());
        });
        return list.isEmpty() ? null : list.get(0);
    }

    /** 把传入 SQL 当作子查询统计行数。 */
    public long countOf(Long dataSourceId, String sql, Map<String, Object> params) {
        SqlReadOnlyValidator.assertSelectOnly(sql);
        String wrapped = "SELECT COUNT(*) FROM (" + sql + ") _sub";
        Long c = jdbc(dataSourceId).queryForObject(wrapped, params, Long.class);
        return c == null ? 0L : c;
    }

    /** 取样本行（截断 + 由调用方做脱敏）。 */
    public List<Map<String, Object>> rows(Long dataSourceId, String sql, Map<String, Object> params, int limit) {
        SqlReadOnlyValidator.assertSelectOnly(sql);
        var tpl = jdbc(dataSourceId);
        tpl.getJdbcTemplate().setMaxRows(Math.max(1, limit));
        return SampleMasker.mask(tpl.queryForList(sql, params));
    }
}
