<template>
  <el-card shadow="never">
    <TableToolbar
      kicker="SYSTEM · 文件"
      title="文件中心"
      desc="上传通用附件并记录元数据；当前本地存储位于公开 /uploads 目录，请勿上传合同、证件等敏感文件。"
    >
      <el-input v-model="query.originalName" placeholder="文件名" clearable style="width: 220px" @keyup.enter="handleSearch" />
      <template #actions>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button v-perm="'sysFile:upload'" type="primary" plain :loading="uploading" @click="pickFile">
          上传文件
        </el-button>
        <input ref="fileInput" type="file" class="hidden-file" @change="handleFileChange" />
      </template>
    </TableToolbar>

    <el-table v-loading="loading" :data="list" stripe border>
      <el-table-column prop="originalName" label="文件名" min-width="220" show-overflow-tooltip />
      <el-table-column prop="fileSize" label="大小" width="110">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="contentType" label="类型" min-width="160" show-overflow-tooltip />
      <el-table-column prop="storageType" label="存储" width="80" />
      <el-table-column prop="createdBy" label="上传人" width="110" />
      <el-table-column prop="createdTime" label="上传时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" tag="a" :href="row.url" target="_blank" rel="noopener">下载</el-button>
          <el-button v-perm="'sysFile:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <PaginationBar :total="total" :page="query.pageNum" :size="query.pageSize" @change="onPageChange" />
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteFile, pageFiles, uploadFile } from '@/api/file'
import PaginationBar from '@/components/PaginationBar.vue'
import TableToolbar from '@/components/TableToolbar.vue'

const loading = ref(false)
const uploading = ref(false)
const list = ref([])
const total = ref(0)
const fileInput = ref()
const query = reactive({ pageNum: 1, pageSize: 10, originalName: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await pageFiles({ ...query })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error('[sysFile] 加载文件失败：', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.originalName = ''
  handleSearch()
}

function onPageChange(page, size) {
  query.pageNum = page
  query.pageSize = size
  loadData()
}

function pickFile() {
  fileInput.value?.click()
}

async function handleFileChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  uploading.value = true
  try {
    await uploadFile(file)
    ElMessage.success('文件已上传')
    await handleSearch()
  } catch (e) {
    console.error('[sysFile] 上传失败：', e)
  } finally {
    uploading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.originalName}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
    })
    await deleteFile(row.id)
    ElMessage.success('文件已删除')
    await handleSearch()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') console.error('[sysFile] 删除失败：', e)
  }
}

function formatSize(bytes) {
  if (bytes == null) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

loadData()
</script>

<style scoped>
.hidden-file {
  display: none;
}
</style>
