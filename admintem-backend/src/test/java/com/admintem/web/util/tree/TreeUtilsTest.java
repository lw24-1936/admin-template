package com.admintem.web.util.tree;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TreeUtils} 单元测试（通用树构建工具）。
 *
 * <p>菜单树/权限树都依赖它：排序稳定性、父子装配、孤儿节点处理、空入参，
 * 任何一处回归都会直接表现为"菜单少了一层"或"节点静默消失"。</p>
 */
@DisplayName("通用树构建 TreeUtils")
class TreeUtilsTest {

    /** 测试用树节点 */
    static class Node extends TreeNode<Node> {
        private final Long id;
        private final Long parentId;
        private final Integer orderNum;
        private final String name;

        Node(Long id, Long parentId, Integer orderNum, String name) {
            this.id = id;
            this.parentId = parentId;
            this.orderNum = orderNum;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public Long getParentId() {
            return parentId;
        }

        @Override
        public Integer getOrderNum() {
            return orderNum;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private List<Node> sample() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(1L, 0L, 1, "工作台"));
        nodes.add(new Node(2L, 0L, 2, "系统管理"));
        nodes.add(new Node(3L, 2L, 1, "用户管理"));
        nodes.add(new Node(4L, 2L, 2, "角色管理"));
        nodes.add(new Node(5L, 2L, 3, "权限管理"));
        return nodes;
    }

    @Test
    @DisplayName("按 parentId 正确装配父子层级")
    void buildsHierarchy() {
        List<Node> tree = TreeUtils.buildTree(sample());
        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).toString()).isEqualTo("工作台");
        assertThat(tree.get(1).toString()).isEqualTo("系统管理");
        assertThat(tree.get(1).getChildren()).hasSize(3);
        assertThat(tree.get(1).getChildren().get(0).toString()).isEqualTo("用户管理");
    }

    @Test
    @DisplayName("同级按 orderNum 升序，orderNum 为空排在最后")
    void sortsByOrderNumWithNullLast() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(10L, 0L, 3, "第三"));
        nodes.add(new Node(11L, 0L, null, "无序号"));
        nodes.add(new Node(12L, 0L, 1, "第一"));
        nodes.add(new Node(13L, 0L, 2, "第二"));

        List<Node> tree = TreeUtils.buildTree(nodes);
        assertThat(tree).extracting(Object::toString)
                .containsExactly("第一", "第二", "第三", "无序号");
    }

    @Test
    @DisplayName("默认不提升孤儿节点：父级缺失时该节点不出现在根层级")
    void orphanNotPromotedByDefault() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(1L, 0L, 1, "工作台"));
        nodes.add(new Node(9L, 999L, 1, "孤儿节点"));

        List<Node> tree = TreeUtils.buildTree(nodes);
        assertThat(tree).extracting(Object::toString).containsExactly("工作台");
    }

    @Test
    @DisplayName("orphanAsRoot=true 时孤儿节点提升为根（授权不全时避免菜单丢失）")
    void orphanPromotedWhenRequested() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(1L, 0L, 1, "工作台"));
        nodes.add(new Node(9L, 999L, 2, "孤儿节点"));

        List<Node> tree = TreeUtils.buildTree(nodes, true);
        assertThat(tree).extracting(Object::toString).containsExactly("工作台", "孤儿节点");
    }

    @Test
    @DisplayName("根节点判定：parentId 为 0 或 null；构建后会标记 root/leaf")
    void marksRootAndLeafFlags() {
        List<Node> tree = TreeUtils.buildTree(sample(), true);
        Node group = tree.get(1);
        assertThat(group.getRootNode()).isTrue();
        assertThat(group.getLeafNode()).isFalse();
        assertThat(group.getChildren().get(0).getLeafNode()).isTrue();
    }

    @Test
    @DisplayName("空入参返回空列表，不抛异常")
    void handlesEmptyInput() {
        assertThat(TreeUtils.buildTree(null)).isEmpty();
        assertThat(TreeUtils.buildTree(new ArrayList<Node>())).isEmpty();
    }
}
