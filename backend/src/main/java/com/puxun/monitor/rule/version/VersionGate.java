package com.puxun.monitor.rule.version;

import com.puxun.monitor.engine.model.RuleScope;

/**
 * 规则生效判定：产品线 ∧ 服务器 ∧ 版本表达式 全部匹配才生效。
 */
public final class VersionGate {

    private VersionGate() {}

    public static boolean effective(RuleScope scope, String productLine, String serverGroup, String deployVersion) {
        if (scope == null) return true;
        boolean productOk = isEmpty(scope.products()) || scope.products().contains(productLine);
        boolean serverOk = isEmpty(scope.servers()) || scope.servers().contains(serverGroup);
        boolean versionOk = VersionSpec.parse(scope.versionExpr()).matches(deployVersion);
        return productOk && serverOk && versionOk;
    }

    /** 返回跳过原因，便于前端展示；生效则返回 null。 */
    public static String skipReason(RuleScope scope, String productLine, String serverGroup, String deployVersion) {
        if (scope == null) return null;
        if (!(isEmpty(scope.products()) || scope.products().contains(productLine))) {
            return "产品线不匹配(" + productLine + ")";
        }
        if (!(isEmpty(scope.servers()) || scope.servers().contains(serverGroup))) {
            return "服务器不匹配(" + serverGroup + ")";
        }
        if (!VersionSpec.parse(scope.versionExpr()).matches(deployVersion)) {
            return "版本未达 " + scope.versionExpr() + "(当前 " + deployVersion + ")";
        }
        return null;
    }

    private static boolean isEmpty(java.util.List<?> l) {
        return l == null || l.isEmpty();
    }
}
