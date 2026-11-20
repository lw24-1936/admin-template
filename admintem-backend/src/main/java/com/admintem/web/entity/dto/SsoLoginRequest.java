package com.admintem.web.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/** 外部网关 SSO 断言登录入参。 */
@Data
@Schema(description = "SSO 断言登录")
public class SsoLoginRequest {

    @NotBlank(message = "SSO 断言不能为空")
    @Size(max = 4096, message = "SSO 断言过长")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "网关签发的 HMAC JWT 断言")
    private String assertion;

    @Pattern(regexp = "^[A-Za-z0-9_-]{8,64}$", message = "设备ID格式不正确")
    @Schema(description = "设备ID（可选）")
    private String deviceId;

    @Schema(description = "是否记住我")
    private Boolean rememberMe = false;
}
