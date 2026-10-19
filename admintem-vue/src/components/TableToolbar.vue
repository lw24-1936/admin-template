<template>
  <!--
    公共列表页工具条（编辑式）
    - intro 区：kicker（模块小标签）+ 标题 + 一行说明，用于给列表页一个明确的层级起点
    - 左侧默认槽放筛选控件，右侧 actions 槽放操作按钮
    - title/desc 均可选：不传时只渲染筛选与操作区（向后兼容既有页面）
  -->
  <div class="table-toolbar-wrap">
    <div v-if="kicker || title || desc" class="table-toolbar__intro">
      <p v-if="kicker" class="table-toolbar__kicker">{{ kicker }}</p>
      <h1 v-if="title" class="table-toolbar__title">{{ title }}</h1>
      <p v-if="desc" class="table-toolbar__desc">{{ desc }}</p>
    </div>

    <div class="table-toolbar">
      <div class="table-toolbar__filters">
        <slot />
      </div>
      <div class="table-toolbar__actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 公共列表工具条组件
 *
 * 用法：
 *   <TableToolbar kicker="SYSTEM · 用户" title="用户管理" desc="维护账号、状态与角色归属">
 *     <el-input v-model="query.userName" placeholder="账号" />
 *     <template #actions>
 *       <el-button type="primary" @click="handleSearch">查询</el-button>
 *       <el-button @click="handleReset">重置</el-button>
 *       <el-button v-perm="'xx:yy'" type="primary" plain>新增</el-button>
 *     </template>
 *   </TableToolbar>
 */
defineProps({
  /** 模块小标签（全大写、字距放大，编辑式排版用） */
  kicker: { type: String, default: '' },
  /** 页面标题 */
  title: { type: String, default: '' },
  /** 一行说明：该页面管什么、权限口径是什么 */
  desc: { type: String, default: '' },
})
</script>

<style scoped>
.table-toolbar-wrap {
  margin-bottom: var(--lc-sp-4);
}
.table-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--lc-sp-3);
  padding-bottom: var(--lc-sp-4);
  border-bottom: 1px solid var(--lc-line-soft);
}
/* 左侧筛选区自动占据剩余宽度，右移/换行更自然 */
.table-toolbar__filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--lc-sp-3);
  flex: 1 1 auto;
}
/*
 * 筛选控件不允许被压缩：控件宽度是页面按字段长度声明的（如日期范围 360px），
 * 一旦被 flex 收缩，日期选择器会被挤成一个图标、输入框也会截断占位符。
 * 放不下时应当换行（上面的 flex-wrap），而不是变形。
 */
.table-toolbar__filters > * {
  flex: 0 0 auto;
}
/* 右侧按钮区 */
.table-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--lc-sp-2);
  flex: 0 0 auto;
}
</style>
