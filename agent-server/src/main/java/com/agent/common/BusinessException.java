package com.agent.common;

/**
 * 可预期的业务异常，由 {@link GlobalExceptionHandler} 转为 {@code code=1} 响应。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
