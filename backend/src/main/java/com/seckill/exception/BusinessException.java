package com.seckill.exception;

import com.seckill.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常，用于可预期的业务逻辑错误
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final String message;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMsg());
        this.code = errorCode.getCode();
        this.message = errorCode.getDefaultMsg();
    }

    public BusinessException(ErrorCode errorCode, String customMsg) {
        super(customMsg != null ? customMsg : errorCode.getDefaultMsg());
        this.code = errorCode.getCode();
        this.message = customMsg != null ? customMsg : errorCode.getDefaultMsg();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
