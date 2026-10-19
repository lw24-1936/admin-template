package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 强制下线入参。
 */
@Data
@Schema(description = "强制下线入参")
public class KickSessionRequest {

    /** 目标账号（必填；不支持按令牌踢，避免前端拿到别人的令牌） */
    @NotBlank(message = "账号不能为空")
    @Size(max = 32, message = "账号长度不能超过 32")
    @Schema(description = "要强制下线的账号", example = "test")
    private String userName;
}
