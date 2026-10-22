package com.admintem.web.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 拼图校验入参。 */
@Data
public class CaptchaVerifyRequest {
    @NotBlank(message = "验证码 token 不能为空")
    private String token;
    @NotNull(message = "拖动位置不能为空")
    private Integer x;
}
