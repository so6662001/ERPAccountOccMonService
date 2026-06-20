package com.puxun.monitor.rule.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("rule_set_item")
public class RuleSetItem implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long ruleSetId;
    private String ruleKey;
    private String effectiveVersionExpr;
}
