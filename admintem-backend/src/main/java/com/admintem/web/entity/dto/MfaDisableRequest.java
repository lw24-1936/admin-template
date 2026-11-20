package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 关闭 MFA：必须同时校验当前密码与 TOTP。 */
@Data
@Schema(description = "关闭 MFA")
public class MfaDisableRequest {

    @NotBlank(message = "密码不能为空")
    @Size(max = 72, message = "密码长度不合法")
    @Schema(description = "当前账号密码")
    private String password;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须是 6 位数字")
    @Schema(description = "6 位 TOTP 验证码", example = "123456")
    private String code;
}
