package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 按用户 ID 查询请求体。
 */
@Data
public class UserIdRequest {

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "3")
    private Long userId;
}
