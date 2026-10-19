<template>
  <!-- 分组节点 -->
  <el-sub-menu v-if="isGroup" :index="String(node.id)">
    <template #title>
      <el-icon><component :is="iconComponent" /></el-icon>
      <span>{{ node.name }}</span>
    </template>
    <MenuItem
      v-for="child in visibleChildren"
      :key="child.id"
      :node="child"
    />
  </el-sub-menu>

  <!-- 叶子节点：有子节点且自身可路由（父菜单即页面） -->
  <el-sub-menu v-else-if="hasChildren && hasUrl" :index="node.menuUrl">
    <template #title>
      <el-icon><component :is="iconComponent" /></el-icon>
      <span>{{ node.name }}</span>
    </template>
    <MenuItem
      v-for="child in visibleChildren"
      :key="child.id"
      :node="child"
    />
  </el-sub-menu>

  <!-- 普通叶子 -->
  <el-menu-item v-else :index="node.menuUrl || String(node.id)">
    <el-icon><component :is="iconComponent" /></el-icon>
    <template #title>{{ node.name }}</template>
  </el-menu-item>
</template>

<script setup>
/**
 * 递归菜单节点组件
 * - 有子节点且自身无路由地址 => 渲染 el-sub-menu 分组
 * - 叶子节点 => 渲染 el-menu-item（index 为路由地址）
 */
import { computed } from 'vue'
import { filterVisibleMenus } from '@/utils/menu'

const props = defineProps({
  node: { type: Object, required: true },
})

/** 旧项目图标名(el-icon-*)到 Element Plus 图标组件名的映射 */
const ICON_MAP = {
  'el-icon-s-home': 'HomeFilled',
  'el-icon-s-operation': 'Operation',
  'el-icon-user': 'User',
  'el-icon-user-solid': 'UserFilled',
  'el-icon-s-check': 'Finished',
  'el-icon-menu': 'Menu',
  'el-icon-s-data': 'DataLine',
  'el-icon-s-custom': 'Avatar',
  'el-icon-s-grid': 'Grid',
  'el-icon-medal': 'Medal',
}

/** 解析出的图标组件名 */
const iconComponent = computed(() => ICON_MAP[props.node.icon] || 'Menu')

/** 是否有可点击的路由地址 */
const hasUrl = computed(() => !!props.node.menuUrl)

/** 是否含有子节点 */
const hasChildren = computed(() => props.node.children && props.node.children.length > 0)

/**
 * 递归渲染用的子节点：过滤 show=1（隐藏）的菜单。
 * 隐藏菜单不在导航中显示，但路由仍然注册，可直接通过 URL 访问。
 */
const visibleChildren = computed(() => filterVisibleMenus(props.node.children))

/** 渲染成子菜单分组（含子节点且本身无路由地址） */
const isGroup = computed(() => hasChildren.value && !hasUrl.value)
</script>

