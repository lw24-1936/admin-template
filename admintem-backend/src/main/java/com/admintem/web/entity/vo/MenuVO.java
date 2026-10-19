package com.admintem.web.entity.vo;

import com.admintem.web.util.tree.TreeNode;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户可访问菜单树视图对象（仅 type=0 的菜单项）。
 *
 * <p>前端据此渲染左侧菜单；每个节点携带 {@code permissionKey}，
 * 用于路由守卫判断页面是否可进入。</p>
 *
 * <p>继承 {@link TreeNode}：{@code children/rootNode/leafNode} 由
 * {@link com.admintem.web.util.tree.TreeUtils} 装配，节点本身只需提供
 * id / parentId / orderNum 三个取值。</p>
 */
@Data
public class MenuVO extends TreeNode<MenuVO> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 菜单ID */
    private Long id;

    /** 菜单名称 */
    private String name;

    /** 类型（恒为 0=菜单；供前端动态路由识别叶子菜单） */
    private Integer type;

    /** 父级ID（0 表示顶级） */
    private Long parentId;

    /** 显示顺序 */
    private Integer orderNum;

    /** 菜单路由地址（前端 route path） */
    private String menuUrl;

    /** 前端组件路径 */
    private String component;

    /** 图标 */
    private String icon;

    /** 展示状态（0展示 1隐藏） */
    private Integer show;

    /** 权限唯一标识 */
    private String permissionKey;
}
