package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 开启/校验 MFA 的 6 位验证码。 */
@Data
@Schema(description = "MFA 验证码")
public class MfaCodeRequest {

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须是 6 位数字")
    @Schema(description = "6 位 TOTP 验证码", example = "123456")
    private String code;
}
