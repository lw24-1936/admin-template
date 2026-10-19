<template>
  <!-- 公共分页条：封装 el-pagination，统一页码/每页条数与变更事件 -->
  <div class="pagination-bar">
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
</template>

<script setup>
/**
 * 公共分页条组件
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
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
