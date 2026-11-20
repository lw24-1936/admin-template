package com.admintem.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色表实体（sys_auth_role）。
 */
@Data
@Schema(description = "角色实体")
public class SysAuthRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色ID */
    @Schema(description = "角色ID")
    private Long id;

    /** 角色名称（中文展示名） */
    @Schema(description = "角色名称")
    private String roleName;

    /** 角色唯一标识（role_key，用于代码中判定，如 admin/common） */
    @Schema(description = "角色唯一标识")
    private String roleKey;

    /** 数据范围：ALL/DEPT/DEPT_AND_CHILD/SELF */
    @Schema(description = "数据范围", example = "ALL")
    private String dataScope;

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
