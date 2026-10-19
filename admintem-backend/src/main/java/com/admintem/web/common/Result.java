package com.admintem.web.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一 API 响应结构。
 *
 * <p>所有控制器返回体统一为 {@code { code, message, data }}：</p>
 * <ul>
 *   <li>{@code code}：业务状态码，200 表示成功，见 {@link ResultCode}；</li>
 *   <li>{@code message}：提示信息（成功/失败原因）；</li>
 *   <li>{@code data}：业务数据，可为 null。</li>
 * </ul>
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应结构")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码 */
    @Schema(description = "业务状态码，200=成功")
    private int code;

    /** 提示信息 */
    @Schema(description = "提示信息")
    private String message;

    /** 业务数据 */
    @Schema(description = "业务数据")
    private T data;

    /* ---------------------------- 成功快捷方法 ---------------------------- */

    /** 成功（无数据） */
    public static <T> Result<T> ok() {
        return build(ResultCode.SUCCESS, null);
    }

    /** 成功（携带数据） */
    public static <T> Result<T> ok(T data) {
        return build(ResultCode.SUCCESS, data);
    }

    /** 成功（自定义提示 + 数据） */
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /* ---------------------------- 失败快捷方法 ---------------------------- */

    /** 失败（使用预置错误码） */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 失败（预置错误码 + 自定义提示） */
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    /** 失败（直接指定码与提示） */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /* ------------------------------ 内部方法 ------------------------------ */

    /** 由 ResultCode 构造响应体 */
    private static <T> Result<T> build(ResultCode resultCode, T data) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), data);
    }

    /**
     * 判断响应是否成功（供内部工具与测试断言使用）。
     *
     * <p>标注 {@code @JsonIgnore} 的原因：这是"计算属性"而非协议字段。
     * Jackson 默认会把 {@code isSuccess} 序列化成 {@code success}，
     * 于是响应体实际是 {@code {code,message,data,success}}，
     * 与《05-接口与权限标识对照表》约定的 {@code {code,message,data}} 不一致——
     * 契约文档与真实报文必须一致，否则前端与第三方对接时会被多出来的字段误导。
     * 若确实需要该字段，应显式加回并同步更新契约文档。</p>
     */
    @JsonIgnore
    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}
