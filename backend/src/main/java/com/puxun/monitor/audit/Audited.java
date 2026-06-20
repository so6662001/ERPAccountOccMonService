package com.puxun.monitor.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写入审计日志的关键写操作。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** 动作，如 EDIT_RULE / APPLY_TEMPLATE / ROLLOUT / RESET_BASELINE / EDIT_DATASOURCE。 */
    String action();

    /** 对象描述（可选，支持简单说明）。 */
    String target() default "";
}
