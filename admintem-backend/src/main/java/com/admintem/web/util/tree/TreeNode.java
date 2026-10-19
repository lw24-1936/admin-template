package com.admintem.web.util.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 树节点基类（参照旧项目 utils/tree/TreeNode 抽象设计并泛型化，更通用）。
 *
 * <p>使用方式：业务节点类继承 {@code TreeNode<T>}（T 为自身类型），
 * 只需提供三个抽象取值方法：</p>
 * <ul>
 *   <li>{@link #getId()}：当前节点 ID；</li>
 *   <li>{@link #getParentId()}：父节点 ID（0 或 null 表示顶级）；</li>
 *   <li>{@link #getOrderNum()}：同级排序值（null 排最后）。</li>
 * </ul>
 *
 * <p>children / rootNode / leafNode 由 {@link TreeUtils} 在装配时自动填充，
 * 业务层无需手动管理子节点，也不必关心排序与父子归属。</p>
 *
 * @param <T> 具体节点类型（自引用泛型，保证 children 类型安全）
 */
public abstract class TreeNode<T extends TreeNode<T>> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 子节点集合（装配前为空；叶子节点的 children 为空列表） */
    private List<T> children = new ArrayList<>();

    /** 是否根节点：parentId 为空或 0（由 TreeUtils 装配时刷新） */
    private Boolean rootNode = Boolean.FALSE;

    /** 是否叶子节点：无子节点（由 TreeUtils 装配时刷新） */
    private Boolean leafNode = Boolean.TRUE;

    /* ------------------------- 子类必须提供 ------------------------- */

    /** 当前节点 ID（子类实现） */
    public abstract Long getId();

    /** 父节点 ID（子类实现，0 或 null 表示顶级） */
    public abstract Long getParentId();

    /** 同级排序值（子类实现，null 排最后） */
    public abstract Integer getOrderNum();

    /* ------------------------- children 访问器 ------------------------- */

    /**
     * 子节点集合（叶子节点为空列表）。
     *
     * @return children
     */
    public List<T> getChildren() {
        return children;
    }

    /**
     * 设置子节点并刷新 rootNode / leafNode 标记。
     *
     * <p>仅供 {@link TreeUtils} 装配调用；允许 null（视为空子节点）。</p>
     *
     * @param children 子节点集合（可为 null）
     */
    public void setChildren(List<T> children) {
        this.children = children == null ? new ArrayList<>() : children;
        this.rootNode = getParentId() == null || Objects.equals(getParentId(), 0L);
        this.leafNode = this.children.isEmpty();
    }

    /* ------------------------- 装配标记（不参与 JSON 输出） ------------------------- */

    /**
     * 是否根节点（parentId 为空或 0）。
     * 属于内存装配标记，@JsonIgnore 保证对外 JSON 结构不变。
     *
     * @return true 表示根节点
     */
    @JsonIgnore
    public Boolean getRootNode() {
        return rootNode;
    }

    /**
     * 是否叶子节点（无子节点）。
     * 属于内存装配标记，@JsonIgnore 保证对外 JSON 结构不变。
     *
     * @return true 表示叶子节点
     */
    @JsonIgnore
    public Boolean getLeafNode() {
        return leafNode;
    }
}
