package com.admintem.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-角色关联表实体（sys_auth_user_role）。
 *
 * <p>一名用户可拥有多个角色，一个角色可被多个用户拥有（多对多关系表）。</p>
 */
@Data
@Schema(description = "用户角色关联实体")
public class SysAuthUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联ID */
    @Schema(description = "关联ID")
    private Long id;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 角色ID */
    @Schema(description = "角色ID")
    private Long roleId;

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
