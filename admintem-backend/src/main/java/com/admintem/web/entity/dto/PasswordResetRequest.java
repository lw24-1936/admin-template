package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 使用验证码重置密码（公开接口）。 */
@Data
@Schema(description = "重置密码")
public class PasswordResetRequest {

    @NotBlank(message = "账号不能为空")
    @Size(max = 32, message = "账号长度不合法")
    @Schema(description = "登录账号", example = "test")
    private String userName;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须是 6 位数字")
    @Schema(description = "6 位重置验证码")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 72, message = "新密码长度必须是 6~72")
    @Schema(description = "新密码")
    private String newPassword;
}
