package com.puxun.monitor.common;

import lombok.Getter;

/**
 * 统一业务错误码。
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "成功"),
    BAD_REQUEST(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "未认证或登录已过期"),
    FORBIDDEN(40300, "无访问权限"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源冲突"),
    READONLY_VIOLATION(42200, "仅允许只读查询"),
    BIZ_ERROR(50000, "业务处理失败"),
    INTERNAL_ERROR(50001, "系统内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
