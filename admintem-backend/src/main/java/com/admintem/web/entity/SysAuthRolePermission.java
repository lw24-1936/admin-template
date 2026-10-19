package com.admintem.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色-权限关联表实体（sys_auth_role_permission）。
 *
 * <p>一个角色可拥有多个权限（菜单 + 按钮），一个权限可被多个角色拥有（多对多关系表）。</p>
 */
@Data
@Schema(description = "角色权限关联实体")
public class SysAuthRolePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联ID */
    @Schema(description = "关联ID")
    private Long id;

    /** 角色ID */
    @Schema(description = "角色ID")
    private Long roleId;

    /** 权限ID */
    @Schema(description = "权限ID")
    private Long permissionId;

    /** 创建人 */
    @Schema(description = "创建人")
    private String createdBy;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /** 更新人 */
    @Schema(description = "更新人")
    private String updateBy;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @JsonIgnore
    @Schema(description = "逻辑删除标记", hidden = true)
    private Integer isDeleted;
}
