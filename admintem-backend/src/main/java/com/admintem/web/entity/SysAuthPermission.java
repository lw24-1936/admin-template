package com.admintem.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限表实体（sys_auth_permission）。
 *
 * <p>权限分两类（{@code type}）：
 * <ul>
 *   <li>0：菜单权限 —— 控制左侧菜单/页面路由是否可见；</li>
 *   <li>1：按钮（操作）权限 —— 控制页面内按钮与对应写接口的访问。
 *       {@code permission_key} 作为全局唯一权限标识，供后端 @PreAuthorize 与前端 v-perm 指令共用。</li>
 * </ul>
 * </p>
 */
@Data
@Schema(description = "权限实体")
public class SysAuthPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 权限ID */
    @Schema(description = "权限ID")
    private Long id;

    /** 权限名称（菜单名/按钮名） */
    @Schema(description = "权限名称")
    private String name;

    /** 父级ID（0 表示顶级） */
    @Schema(description = "父级ID，0 表示顶级")
    private Long parentId;

    /** 显示顺序 */
    @Schema(description = "显示顺序")
    private Integer orderNum;

    /** 权限类型：0 菜单，1 按钮（操作） */
    @Schema(description = "权限类型：0 菜单，1 按钮/操作")
    private Integer type;

    /** 菜单路由地址（前端 route path，仅菜单类型使用） */
    @Schema(description = "菜单路由地址")
    private String menuUrl;

    /** 组件路径（前端页面组件标识，仅菜单类型使用） */
    @Schema(description = "前端组件路径")
    private String component;

    /** 后端接口地址（该按钮/菜单对应的后端接口，用于审计与展示） */
    @Schema(description = "后端接口地址")
    private String backendUrl;

    /** 状态（0启用 1禁用） */
    @Schema(description = "状态（0启用 1禁用）")
    private Integer status;

    /** 展示状态（0展示 1隐藏，菜单类型使用） */
    @Schema(description = "展示状态（0展示 1隐藏）")
    private Integer show;

    /** 图标标识（前端图标 key） */
    @Schema(description = "图标标识")
    private String icon;

    /** 权限唯一标识（如 userInfo:userInfo、sysAuthUser:addSysAuthUser） */
    @Schema(description = "权限唯一标识")
    private String permissionKey;

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
