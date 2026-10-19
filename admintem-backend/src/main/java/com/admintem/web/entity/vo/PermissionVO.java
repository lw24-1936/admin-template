package com.admintem.web.entity.vo;

import com.admintem.web.util.tree.TreeNode;
import lombok.Data;

import java.io.Serializable;

/**
 * 权限树节点视图对象（菜单 + 按钮统一树形结构）。
 *
 * <p>由 sys_auth_permission 全表构建：type=0 为菜单节点、type=1 为按钮子节点，
 * 供「权限管理」页与「角色授权」树形勾选使用。</p>
 *
 * <p>继承 {@link TreeNode}：{@code children/rootNode/leafNode} 由
 * {@link com.admintem.web.util.tree.TreeUtils} 装配，节点本身只需提供
 * id / parentId / orderNum 三个取值。</p>
 */
@Data
public class PermissionVO extends TreeNode<PermissionVO> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 权限ID */
    private Long id;

    /** 权限名称 */
    private String name;

    /** 父级ID */
    private Long parentId;

    /** 显示顺序 */
    private Integer orderNum;

    /** 类型：0 菜单，1 按钮 */
    private Integer type;

    /** 菜单路由 */
    private String menuUrl;

    /** 前端组件路径 */
    private String component;

    /** 后端接口地址 */
    private String backendUrl;

    /** 状态（0启用 1禁用） */
    private Integer status;

    /** 展示状态（0展示 1隐藏） */
    private Integer show;

    /** 图标 */
    private String icon;

    /** 权限唯一标识 */
    private String permissionKey;
}
