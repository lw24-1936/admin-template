package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 按角色 ID 查询请求体。
 */
@Data
public class RoleIdRequest {

    /** 角色ID */
    @NotNull(message = "角色ID不能为空")
    @Schema(description = "角色ID", example = "2")
    private Long roleId;
}
