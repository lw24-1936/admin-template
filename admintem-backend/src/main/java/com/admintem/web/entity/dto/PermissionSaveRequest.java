package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.admintem.web.entity.dto.validation.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 权限（菜单/按钮）新增或编辑请求体。
 */
@Data
public class PermissionSaveRequest {

    /** 权限ID（编辑时必填；分组校验：仅 Update 分组要求非空） */
    @NotNull(message = "ID不能为空", groups = ValidationGroups.Update.class)
    @Schema(description = "权限ID（编辑时必填；分组校验：仅 Update 分组要求非空）", example = "3")
    private Long id;

    /** 权限名称 */
    @NotBlank(message = "权限名称不能为空")
    @Schema(description = "权限名称", example = "新增用户")
    private String name;

    /** 父级ID（0 顶级） */
    @NotNull(message = "父级ID不能为空")
    @Schema(description = "父级ID（0 顶级）", example = "3")
    private Long parentId;

    /** 显示顺序 */
    @Schema(description = "显示顺序", example = "10")
    private Integer orderNum;

    /** 类型：0 菜单，1 按钮/操作 */
    @NotNull(message = "权限类型不能为空")
    @Schema(description = "类型：0 菜单，1 按钮/操作", example = "1")
    private Integer type;

    /** 菜单路由地址 */
    @Schema(description = "菜单路由地址", example = "/userInfo")
    private String menuUrl;

    /** 前端组件路径 */
    @Schema(description = "前端组件路径", example = "userInfo/UserInfo")
    private String component;

    /** 后端接口地址 */
    @Schema(description = "后端接口地址", example = "/sysAuthUser/addSysAuthUser")
    private String backendUrl;

    /** 状态（0启用 1禁用） */
    @Schema(description = "状态（0启用 1禁用）", example = "0")
    private Integer status;

    /** 展示状态（0展示 1隐藏） */
    @Schema(description = "展示状态（0展示 1隐藏）", example = "0")
    private Integer show;

    /** 图标标识 */
    @Schema(description = "图标标识", example = "User")
    private String icon;

    /** 权限唯一标识 */
    @NotBlank(message = "权限标识不能为空")
    @Schema(description = "权限唯一标识", example = "sysAuthUser:addSysAuthUser")
    private String permissionKey;
}
