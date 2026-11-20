<template>
  <div class="pro-table">
    <el-table
      v-loading="loading"
      :data="data"
      :row-key="rowKey"
      stripe
      border
      @selection-change="$emit('selection-change', $event)"
      @sort-change="$emit('sort-change', $event)"
    >
      <el-table-column v-if="selection" type="selection" width="46" />
      <slot />
    </el-table>
    <PaginationBar
      v-if="total > 0"
      :total="total"
      :page="page"
      :size="size"
      @change="onPageChange"
    />
  </div>
</template>

<script setup>
import PaginationBar from '@/components/PaginationBar.vue'

defineProps({
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 },
  rowKey: { type: [String, Function], default: 'id' },
  selection: { type: Boolean, default: false },
})

const emit = defineEmits(['selection-change', 'sort-change', 'page-change'])

function onPageChange(page, size) {
  emit('page-change', page, size)
}
</script>

<style scoped>
.pro-table {
  width: 100%;
}
</style>
