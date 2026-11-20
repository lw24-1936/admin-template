<template>
  <el-card shadow="never">
    <TableToolbar
      kicker="SYSTEM · 字典"
      title="数据字典"
      desc="统一维护状态、类型等下拉项；同类型下字典值唯一，删除采用逻辑删除。"
    >
      <el-input v-model="query.dictType" placeholder="字典类型" clearable style="width: 170px" @keyup.enter="handleSearch" />
      <el-input v-model="query.dictLabel" placeholder="显示文案" clearable style="width: 160px" @keyup.enter="handleSearch" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="handleSearch">
        <el-option label="启用" :value="0" />
        <el-option label="禁用" :value="1" />
      </el-select>
      <template #actions>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button v-perm="'sysDict:add'" type="primary" plain @click="openAdd">新增字典</el-button>
      </template>
    </TableToolbar>

    <el-table v-loading="loading" :data="list" stripe border>
      <el-table-column prop="dictType" label="字典类型" min-width="150" />
      <el-table-column prop="dictLabel" label="显示文案" min-width="140" />
      <el-table-column prop="dictValue" label="字典值" min-width="110" />
      <el-table-column prop="sortNum" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'info'" size="small">
            {{ row.status === 0 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column prop="updateTime" label="更新时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button v-perm="'sysDict:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-perm="'sysDict:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <PaginationBar :total="total" :page="query.pageNum" :size="query.pageSize" @change="onPageChange" />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="form.dictType" placeholder="如 user_status" />
        </el-form-item>
        <el-form-item label="显示文案" prop="dictLabel">
          <el-input v-model="form.dictLabel" placeholder="如 启用" />
        </el-form-item>
        <el-form-item label="字典值" prop="dictValue">
          <el-input v-model="form.dictValue" placeholder="如 0" />
        </el-form-item>
        <el-form-item label="排序" prop="sortNum">
          <el-input-number v-model="form.sortNum" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" :value="0" />
            <el-option label="禁用" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保 存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addDict, deleteDict, editDict, pageDicts } from '@/api/dict'
import PaginationBar from '@/components/PaginationBar.vue'
import TableToolbar from '@/components/TableToolbar.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, dictType: '', dictLabel: '', status: null })

async function loadData() {
  loading.value = true
  try {
    const res = await pageDicts({ ...query })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error('[sysDict] 加载字典失败：', e)
  } finally {
    loading.value = false
  }
}

function onPageChange(page, size) {
  query.pageNum = page
  query.pageSize = size
  loadData()
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.dictType = ''
  query.dictLabel = ''
  query.status = null
  handleSearch()
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增字典')
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, dictType: '', dictLabel: '', dictValue: '', sortNum: 0, status: 0, remark: '' })
const rules = {
  dictType: [
    { required: true, message: '请输入字典类型', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]{1,31}$/, message: '小写字母开头，可含数字与下划线', trigger: 'blur' },
  ],
  dictLabel: [{ required: true, message: '请输入显示文案', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }],
}

function openAdd() {
  dialogTitle.value = '新增字典'
  Object.assign(form, { id: null, dictType: '', dictLabel: '', dictValue: '', sortNum: 0, status: 0, remark: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  dialogTitle.value = '编辑字典'
  Object.assign(form, {
    id: row.id,
    dictType: row.dictType,
    dictLabel: row.dictLabel,
    dictValue: row.dictValue,
    sortNum: row.sortNum ?? 0,
    status: row.status ?? 0,
    remark: row.remark || '',
  })
  dialogVisible.value = true
}

async function saveForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || saving.value) return
  saving.value = true
  try {
    if (form.id) {
      await editDict({ ...form })
      ElMessage.success('字典已更新')
    } else {
      await addDict({ ...form })
      ElMessage.success('字典已创建')
    }
    dialogVisible.value = false
    await handleSearch()
  } catch (e) {
    console.error('[sysDict] 保存字典失败：', e)
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.dictLabel}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
    })
    await deleteDict(row.id)
    ElMessage.success('字典已删除')
    await handleSearch()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') console.error('[sysDict] 删除字典失败：', e)
  }
}

loadData()
</script>
