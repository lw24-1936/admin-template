<template>
  <!-- 固定分页条：贴在视口右下角，列表滚动时始终可见。
       Teleport 到 body：页面容器有入场 transform，会让 fixed 相对容器定位。 -->
  <Teleport to="body">
    <div class="pagination-bar" role="navigation" aria-label="分页">
    <el-pagination
      v-model:current-page="innerPage"
      v-model:page-size="innerSize"
      :total="total"
      :page-sizes="pageSizes"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @change="handleChange"
      />
    </div>
  </Teleport>
  <!-- 占位：避免最后一行数据被浮动分页条遮住 -->
  <div class="pagination-spacer" aria-hidden="true" />
</template>

<script setup>
/**
 * 公共分页条组件
 *
 * 固定在页面右下角（position: fixed），列表滚动时不用回到页底翻页；
 * 组件末尾的 spacer 预留高度，避免遮挡表格最后一行。
 *
 * 用法：
 *   <PaginationBar :total="total" :page="query.pageNum" :size="query.pageSize"
 *                  @change="onPageChange" />
 *   function onPageChange(page, size) { query.pageNum = page; query.pageSize = size; loadData() }
 */
import { ref, watch } from 'vue'

const props = defineProps({
  /** 总记录数 */
  total: { type: Number, default: 0 },
  /** 当前页码（受控，父组件传入） */
  page: { type: Number, default: 1 },
  /** 每页条数（受控，父组件传入） */
  size: { type: Number, default: 10 },
  /** 每页条数可选项 */
  pageSizes: { type: Array, default: () => [10, 20, 50, 100] },
})

const emit = defineEmits(['change'])

// 本地内部值（与父组件受控值同步）
const innerPage = ref(props.page)
const innerSize = ref(props.size)

// 父组件值变化（如查询后重置页码）时同步本地
watch(
  () => props.page,
  (v) => {
    innerPage.value = v
  }
)
watch(
  () => props.size,
  (v) => {
    innerSize.value = v
  }
)

/** 分页变化（页码或每页条数）统一向外抛出 */
function handleChange(page, size) {
  innerPage.value = page
  innerSize.value = size
  emit('change', page, size)
}
</script>

<style scoped>
.pagination-bar {
  position: fixed;
  right: 28px;
  bottom: 20px;
  z-index: 30;
  display: flex;
  justify-content: flex-end;
  padding: 10px 14px;
  border: 1px solid var(--lc-line);
  border-radius: 14px;
  background: var(--lc-paper-card);
  box-shadow: var(--lc-shadow-float);
}

.pagination-spacer {
  height: 68px;
}

/* 窄屏：贴边显示，允许横向滚动，避免把分页挤出视口 */
@media (max-width: 900px) {
  .pagination-bar {
    left: 12px;
    right: 12px;
    bottom: 12px;
    max-width: calc(100vw - 24px);
    overflow-x: auto;
  }
}
</style>
