package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 登录请求体。
 */
@Data
public class LoginRequest {

    /** 登录账号 */
    @NotBlank(message = "账号不能为空")
    @Schema(description = "登录账号", example = "admin")
    private String username;

    /** 登录密码 */
    @NotBlank(message = "密码不能为空")
    @Schema(description = "登录密码", example = "12345678")
    private String password;

    /** 是否记住我（延长令牌有效期） */
    @Schema(description = "是否记住我（延长令牌有效期）", example = "true")
    private Boolean rememberMe = false;

    /** 图形验证码一次性票据（captcha.enabled=true 时必填） */
    @Schema(description = "图形验证码票据")
    private String captchaTicket;

    /**
     * 设备 ID（可选）。传了则按设备独立保存访问令牌，支持同一账号多端在线；
     * 不传则保持旧版单账号单令牌语义。
     */
    @Pattern(regexp = "^[A-Za-z0-9_-]{8,64}$", message = "设备ID格式不正确")
    @Schema(description = "设备ID（可选；8~64 位字母/数字/下划线/中划线）",
            example = "web-7f3b9c2e")
    private String deviceId;
}
