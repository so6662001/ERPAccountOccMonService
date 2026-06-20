package com.puxun.monitor.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puxun.monitor.common.enums.Enums;
import com.puxun.monitor.common.enums.Severity;
import com.puxun.monitor.engine.model.EngineModels.RuleSpec;
import com.puxun.monitor.engine.model.RuleScope;
import com.puxun.monitor.rule.domain.Rule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 将存储的 Rule(spec_json/scope_json) 组装为引擎可执行的 RuleSpec。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleSpecAssembler {

    private final ObjectMapper om;

    public RuleSpec assemble(Rule r) {
        JsonNode spec = readTree(r.getSpecJson());
        RuleScope scope = parseScope(r.getScopeJson());
        return new RuleSpec(
                r.getRuleKey(), r.getName(),
                Enums.RuleCategory.valueOf(r.getCategory()),
                Enums.RuleType.valueOf(r.getType()),
                Severity.valueOf(r.getSeverity()),
                decimal(spec, "tolerance"),
                asLong(spec, "datasourceId"),
                text(spec, "sql"),
                text(spec, "leftSql"),
                text(spec, "rightSql"),
                decimal(spec, "min"),
                decimal(spec, "max"),
                asLong(spec, "leftDatasourceId"),
                asLong(spec, "rightDatasourceId"),
                scope,
                r.getInvariant() != null && r.getInvariant() == 1,
                r.getInvariantMetric()
        );
    }

    private RuleScope parseScope(String json) {
        JsonNode n = readTree(json);
        if (n == null || n.isNull()) return RuleScope.empty();
        return new RuleScope(
                stringList(n.get("products")),
                n.hasNonNull("versionExpr") ? n.get("versionExpr").asText() : "*",
                stringList(n.get("servers")),
                stringList(n.get("ruleSets")),
                longList(n.get("customers"))
        );
    }

    private JsonNode readTree(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return om.readTree(json);
        } catch (Exception e) {
            log.warn("解析规则 JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    private String text(JsonNode n, String f) {
        return n != null && n.hasNonNull(f) ? n.get(f).asText() : null;
    }

    private BigDecimal decimal(JsonNode n, String f) {
        return n != null && n.hasNonNull(f) ? new BigDecimal(n.get(f).asText()) : null;
    }

    private Long asLong(JsonNode n, String f) {
        return n != null && n.hasNonNull(f) ? n.get(f).asLong() : null;
    }

    private List<String> stringList(JsonNode arr) {
        List<String> list = new ArrayList<>();
        if (arr != null && arr.isArray()) arr.forEach(x -> list.add(x.asText()));
        return list;
    }

    private List<Long> longList(JsonNode arr) {
        List<Long> list = new ArrayList<>();
        if (arr != null && arr.isArray()) arr.forEach(x -> list.add(x.asLong()));
        return list;
    }
}
