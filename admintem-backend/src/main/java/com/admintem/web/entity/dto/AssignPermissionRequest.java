package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色权限分配请求体（全量覆盖式分配）。
 */
@Data
public class AssignPermissionRequest {

    /** 角色ID */
    @NotNull(message = "角色ID不能为空")
    @Schema(description = "角色ID", example = "2")
    private Long roleId;

    /** 权限 ID 列表（为空表示清空该角色所有权限） */
    @Schema(description = "权限 ID 列表（为空表示清空该角色所有权限）", example = "[1,3,7]")
    private List<Long> permissionIds;
}
