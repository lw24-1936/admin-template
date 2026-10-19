package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户角色分配请求体（全量覆盖式分配）。
 */
@Data
public class AssignRoleRequest {

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "3")
    private Long userId;

    /** 角色 ID 列表（为空表示清空该用户所有角色） */
    @Schema(description = "角色 ID 列表（为空表示清空该用户所有角色）", example = "[2,3]")
    private List<Long> roleIds;
}
