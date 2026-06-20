package com.puxun.monitor.common;

import lombok.Data;
import org.slf4j.MDC;

import java.io.Serializable;

/**
 * 统一返回体。code=0 表示成功，非 0 表示业务错误。
 */
@Data
public class ApiResult<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private String traceId;

    public ApiResult() {
        this.traceId = MDC.get("traceId");
    }

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = ResultCode.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        ApiResult<T> r = new ApiResult<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> ApiResult<T> fail(ResultCode rc) {
        return fail(rc.getCode(), rc.getMessage());
    }
}
