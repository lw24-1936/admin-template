package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 请求密码重置验证码（公开接口）。 */
@Data
@Schema(description = "请求密码重置验证码")
public class PasswordForgotRequest {

    @NotBlank(message = "账号不能为空")
    @Size(max = 32, message = "账号长度不合法")
    @Schema(description = "登录账号", example = "test")
    private String userName;
}
