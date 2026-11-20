package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 校验 MFA 验证码（绑定挑战票据）。 */
@Data
@Schema(description = "MFA 验证码校验")
public class MfaVerifyRequest {

    @NotBlank(message = "MFA 票据不能为空")
    @Schema(description = "密码登录返回的 MFA 挑战票据")
    private String mfaToken;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须是 6 位数字")
    @Schema(description = "6 位 TOTP 验证码", example = "123456")
    private String code;
}
