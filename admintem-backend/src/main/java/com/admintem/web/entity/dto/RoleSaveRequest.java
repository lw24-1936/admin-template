package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.admintem.web.entity.dto.validation.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 角色新增/编辑请求体。
 */
@Data
public class RoleSaveRequest {

    /** 角色ID（编辑时必填；分组校验：仅 Update 分组要求非空） */
    @NotNull(message = "ID不能为空", groups = ValidationGroups.Update.class)
    @Schema(description = "角色ID（编辑时必填；分组校验：仅 Update 分组要求非空）", example = "3")
    private Long id;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 32, message = "角色名称最长32个字符")
    @Schema(description = "角色名称", example = "运营专员")
    private String roleName;

    /** 角色唯一标识 */
    @NotBlank(message = "角色标识不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,31}$", message = "角色标识须为小写字母开头的字母数字下划线，2~32位")
    @Schema(description = "角色唯一标识", example = "operator")
    private String roleKey;

    /** 数据范围（行级数据权限） */
    @Pattern(regexp = "ALL|DEPT|DEPT_AND_CHILD|SELF", message = "数据范围只能是 ALL/DEPT/DEPT_AND_CHILD/SELF")
    @Schema(description = "数据范围", example = "ALL", allowableValues = {"ALL", "DEPT", "DEPT_AND_CHILD", "SELF"})
    private String dataScope = "ALL";
}
