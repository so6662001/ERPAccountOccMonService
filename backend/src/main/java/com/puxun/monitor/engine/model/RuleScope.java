package com.puxun.monitor.engine.model;

import java.util.List;

/**
 * 规则适用范围（来自 rule.scope_json）。
 */
public record RuleScope(
        List<String> products,     // 适用产品线；空=不限
        String versionExpr,        // 生效版本表达式；空/"*"=不限
        List<String> servers,      // 适用服务器/集群；空=不限
        List<String> ruleSets,     // 归属规则集
        List<Long> customers        // 指定客户（可空）
) {
    public static RuleScope empty() {
        return new RuleScope(List.of(), "*", List.of(), List.of(), List.of());
    }
}
