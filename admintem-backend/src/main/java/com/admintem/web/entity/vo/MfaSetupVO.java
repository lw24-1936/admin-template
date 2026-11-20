package com.admintem.web.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** MFA 开启引导：secret + otpauth URI（只返回一次）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MFA 开启引导")
public class MfaSetupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Base32 secret；只返回一次，请立即录入认证器")
    private String secret;

    @Schema(description = "otpauth:// URI，可渲染二维码")
    private String otpauthUri;
}
