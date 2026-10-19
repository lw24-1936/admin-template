package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改本人密码请求体。
 */
@Data
public class UpdatePasswordRequest {

    /** 原密码 */
    @NotBlank(message = "原密码不能为空")
    @Schema(description = "原密码", example = "12345678")
    private String oldPassword;

    /** 新密码（6~32 位） */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度须为6~32位")
    @Schema(description = "新密码（6~32 位）", example = "Abc123456")
    private String newPassword;
}
