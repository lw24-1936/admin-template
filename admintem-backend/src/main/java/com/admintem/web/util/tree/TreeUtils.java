package com.admintem.web.util.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通用树形数据构建工具（参照旧项目 utils/tree/TreeUtils，泛型化并增强健壮性）。
 *
 * <p>能力：
 * <ul>
 *   <li>自动按 {@code orderNum} 升序排序（null 排最后），同级再按 {@code id} 升序保证稳定；</li>
 *   <li>按 {@code parentId} 分组后为每个节点填充 {@code children}，并刷新
 *       rootNode / leafNode 标记；</li>
 *   <li>只返回根节点列表；对“父节点缺失”的孤儿节点可配置是否提升为根（默认丢弃，
 *       与原项目一致；提升见 {@link #buildTree(List, boolean)}）；</li>
 *   <li>自动判空、过滤 null 元素，不抛 NPE。</li>
 * </ul>
 * </p>
 *
 * <p>示例：{@code List<MenuVO> tree = TreeUtils.buildTree(menuVos, true);}</p>
 */
public final class TreeUtils {

    private TreeUtils() {
    }

    /**
     * 构建树（默认：孤儿节点不提升，仅返回 parentId 为 0/null 的根节点）。
     *
     * @param nodes 扁平节点列表（可含 null，可乱序）
     * @param <T>   节点类型
     * @return 根节点列表（已排序、children 已填充）
     */
    public static <T extends TreeNode<T>> List<T> buildTree(List<T> nodes) {
        return buildTree(nodes, false);
    }

    /**
     * 构建树。
     *
     * @param nodes        扁平节点列表（可含 null，可乱序）
     * @param orphanAsRoot 孤儿节点（parentId 非 0 但父节点不在列表中）是否提升为根；
     *                     true 防止授权不全时菜单丢失，false 与原项目语义一致（丢弃孤儿）
     * @param <T>          节点类型
     * @return 根节点列表（已排序、children 已填充）
     */
    public static <T extends TreeNode<T>> List<T> buildTree(List<T> nodes, boolean orphanAsRoot) {
        // 判空 + 过滤 null
        List<T> all = Optional.ofNullable(nodes).orElseGet(Collections::emptyList).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (all.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 排序：orderNum 升序（null 排最后），同级按 id 升序，保证顺序稳定
        List<T> sorted = all.stream()
                .sorted(Comparator
                        .comparing((T n) -> Optional.ofNullable(n.getOrderNum()).orElse(Integer.MAX_VALUE))
                        .thenComparing(n -> Optional.ofNullable(n.getId()).orElse(Long.MAX_VALUE)))
                .collect(Collectors.toList());

        // 2. 已存在节点 ID 集合（用于孤儿判定）
        Map<Long, T> byId = sorted.stream()
                .filter(n -> n.getId() != null)
                .collect(Collectors.toMap(TreeNode::getId, n -> n, (a, b) -> a, LinkedHashMap::new));

        // 3. 按 parentId 分组（null 按 0 处理）
        Map<Long, List<T>> groupByParent = sorted.stream().collect(Collectors.groupingBy(
                n -> Optional.ofNullable(n.getParentId()).orElse(0L),
                LinkedHashMap::new,
                Collectors.toList()));

        // 4. 为每个节点填充 children（同时刷新 rootNode/leafNode 标记）
        for (T node : sorted) {
            List<T> children = groupByParent.getOrDefault(
                    Optional.ofNullable(node.getId()).orElse(Long.MIN_VALUE), Collections.emptyList());
            node.setChildren(children);
        }

        // 5. 筛选根节点：parentId 为 0/null；或孤儿且允许提升
        return sorted.stream()
                .filter(n -> isRoot(n, byId, orphanAsRoot))
                .collect(Collectors.toList());
    }

    /* ------------------------------ 私有方法 ------------------------------ */

    /** 是否视为根节点 */
    private static <T extends TreeNode<T>> boolean isRoot(T node, Map<Long, T> byId, boolean orphanAsRoot) {
        Long parentId = node.getParentId();
        // 顶级节点
        if (parentId == null || parentId == 0L) {
            return true;
        }
        // 孤儿节点：父节点不在本次节点列表中
        if (!byId.containsKey(parentId)) {
            return orphanAsRoot;
        }
        return false;
    }
}
