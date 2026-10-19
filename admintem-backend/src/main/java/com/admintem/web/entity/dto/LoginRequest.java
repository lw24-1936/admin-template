package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
}
