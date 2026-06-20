package com.puxun.monitor.common;

import lombok.Getter;

/**
 * 业务异常：携带业务错误码，由全局异常处理器转为 ApiResult。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }

    public BizException(ResultCode rc, String message) {
        super(message);
        this.code = rc.getCode();
    }

    public static BizException of(ResultCode rc, String message) {
        return new BizException(rc, message);
    }
}
