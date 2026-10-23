package com.admintem.web.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 图形验证码校验入参。 */
@Data
public class CaptchaVerifyRequest {
    @NotBlank(message = "验证码 token 不能为空")
    private String token;

    @NotBlank(message = "请输入验证码")
    private String code;
}
