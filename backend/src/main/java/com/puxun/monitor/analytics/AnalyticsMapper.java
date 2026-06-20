package com.puxun.monitor.analytics;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 统计聚合查询（MySQL 方言）。
 */
@Mapper
public interface AnalyticsMapper {

    @Select("""
            SELECT DATE(started_at) AS d,
                   COALESCE(SUM(total),0)  AS total,
                   COALESCE(SUM(passed),0) AS passed,
                   COALESCE(SUM(failed),0) AS failed
            FROM detect_run
            WHERE started_at >= #{since}
            GROUP BY DATE(started_at)
            ORDER BY d
            """)
    List<Map<String, Object>> passRateTrend(@Param("since") Instant since);

    @Select("""
            SELECT DATE(dr.started_at) AS d, cr.category AS category, COUNT(*) AS cnt
            FROM check_result cr JOIN detect_run dr ON cr.run_id = dr.id
            WHERE cr.status = 'FAILED' AND dr.started_at >= #{since}
            GROUP BY DATE(dr.started_at), cr.category
            ORDER BY d
            """)
    List<Map<String, Object>> failuresByCategory(@Param("since") Instant since);

    @Select("""
            SELECT cr.rule_key AS ruleKey, cr.rule_name AS ruleName,
                   COUNT(*) AS failures, COUNT(DISTINCT dr.customer_id) AS customers
            FROM check_result cr JOIN detect_run dr ON cr.run_id = dr.id
            WHERE cr.status = 'FAILED' AND dr.started_at >= #{since}
            GROUP BY cr.rule_key, cr.rule_name
            ORDER BY failures DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> topFailingRules(@Param("since") Instant since, @Param("limit") int limit);

    @Select("""
            SELECT c.industry AS industry,
                   COALESCE(SUM(dr.total),0)  AS total,
                   COALESCE(SUM(dr.passed),0) AS passed
            FROM detect_run dr JOIN customer c ON dr.customer_id = c.id
            WHERE dr.started_at >= #{since}
            GROUP BY c.industry
            """)
    List<Map<String, Object>> industryHealth(@Param("since") Instant since);

    @Select("""
            SELECT AVG(TIMESTAMPDIFF(MINUTE, created_at, closed_at))
            FROM alert WHERE status = 'CLOSED' AND closed_at >= #{since}
            """)
    Double mttrMinutes(@Param("since") Instant since);

    @Select("""
            SELECT DATE(started_at) AS d,
                   COALESCE(SUM(total),0)  AS total,
                   COALESCE(SUM(passed),0) AS passed,
                   COALESCE(SUM(failed),0) AS failed
            FROM detect_run
            WHERE customer_id = #{customerId} AND started_at >= #{since}
            GROUP BY DATE(started_at)
            ORDER BY d
            """)
    List<Map<String, Object>> customerTrend(@Param("customerId") Long customerId, @Param("since") Instant since);

    @Select("""
            SELECT COALESCE(SUM(total),0)  AS total,
                   COALESCE(SUM(passed),0) AS passed,
                   COALESCE(SUM(failed),0) AS failed,
                   COALESCE(SUM(errored),0) AS errored,
                   COUNT(*) AS runs
            FROM detect_run WHERE started_at >= #{since}
            """)
    Map<String, Object> execSummary(@Param("since") Instant since);

    @Select("""
            SELECT severity AS severity, COUNT(*) AS cnt
            FROM alert WHERE status IN ('PENDING','PROCESSING')
            GROUP BY severity
            """)
    List<Map<String, Object>> openAlertBySeverity();
}
