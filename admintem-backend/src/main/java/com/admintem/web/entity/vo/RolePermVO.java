package com.admintem.web.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色-权限视图：角色已勾选权限 ID + 全量权限树（用于授权弹窗）。
 */
@Data
public class RolePermVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色ID */
    private Long roleId;

    /** 角色已拥有的权限 ID 列表 */
    private List<Long> permissionIds = new ArrayList<>();

    /** 全量权限树（菜单 + 按钮） */
    private List<PermissionVO> permissionTree = new ArrayList<>();
}
