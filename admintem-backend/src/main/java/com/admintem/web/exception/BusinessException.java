package com.admintem.web.exception;

import com.admintem.web.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常。
 *
 * <p>Service / Controller 中遇到可预期的业务错误时抛出该异常，
 * 由 {@link com.admintem.web.exception.GlobalExceptionHandler} 统一转换为
 * {@link com.admintem.web.common.Result} 返回，避免每个方法重复 try-catch。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务状态码 */
    private final int code;

    /**
     * 使用预置错误码构造。
     *
     * @param resultCode 预置错误码（code + 默认提示）
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 使用预置错误码 + 自定义提示构造。
     *
     * @param resultCode 预置错误码
     * @param message    自定义提示信息
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
