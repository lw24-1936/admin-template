<template>
  <el-card shadow="never">
    <TableToolbar
      kicker="SYSTEM · 参数"
      title="系统配置"
      desc="维护系统级键值参数；变更会立即对读取方生效，删除采用逻辑删除，键名保持唯一。"
    >
      <el-input v-model="query.configKey" placeholder="参数键" clearable style="width: 180px" @keyup.enter="handleSearch" />
      <el-input v-model="query.configName" placeholder="参数名称" clearable style="width: 160px" @keyup.enter="handleSearch" />
      <el-input v-model="query.configGroup" placeholder="分组" clearable style="width: 130px" @keyup.enter="handleSearch" />
      <template #actions>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button v-perm="'sysConfig:add'" type="primary" plain @click="openAdd">新增参数</el-button>
      </template>
    </TableToolbar>

    <el-table v-loading="loading" :data="list" stripe border>
      <el-table-column prop="configKey" label="参数键" min-width="190" show-overflow-tooltip />
      <el-table-column prop="configName" label="名称" min-width="150" />
      <el-table-column prop="configGroup" label="分组" width="110" />
      <el-table-column prop="configValue" label="参数值" min-width="220" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column prop="updateTime" label="更新时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button v-perm="'sysConfig:edit'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-perm="'sysConfig:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <PaginationBar :total="total" :page="query.pageNum" :size="query.pageSize" @change="onPageChange" />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="参数键" prop="configKey">
          <el-input v-model="form.configKey" placeholder="如 system.upload.max-size" />
        </el-form-item>
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" placeholder="如 上传大小上限" />
        </el-form-item>
        <el-form-item label="分组" prop="configGroup">
          <el-input v-model="form.configGroup" placeholder="如 upload" />
        </el-form-item>
        <el-form-item label="参数值" prop="configValue">
          <el-input v-model="form.configValue" type="textarea" :rows="3" placeholder="参数值" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="用途说明" />
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
import { addConfig, deleteConfig, editConfig, pageConfigs } from '@/api/config'
import PaginationBar from '@/components/PaginationBar.vue'
import TableToolbar from '@/components/TableToolbar.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, configKey: '', configName: '', configGroup: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await pageConfigs({ ...query })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error('[sysConfig] 加载参数失败：', e)
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
  query.configKey = ''
  query.configName = ''
  query.configGroup = ''
  handleSearch()
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增参数')
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, configKey: '', configName: '', configGroup: 'default', configValue: '', remark: '' })
const rules = {
  configKey: [
    { required: true, message: '请输入参数键', trigger: 'blur' },
    { pattern: /^[A-Za-z][A-Za-z0-9._-]{1,63}$/, message: '字母开头，可含数字、点、下划线、中划线', trigger: 'blur' },
  ],
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
}

function openAdd() {
  dialogTitle.value = '新增参数'
  Object.assign(form, { id: null, configKey: '', configName: '', configGroup: 'default', configValue: '', remark: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  dialogTitle.value = '编辑参数'
  Object.assign(form, {
    id: row.id,
    configKey: row.configKey,
    configName: row.configName,
    configGroup: row.configGroup || 'default',
    configValue: row.configValue || '',
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
      await editConfig({ ...form })
      ElMessage.success('参数已更新')
    } else {
      await addConfig({ ...form })
      ElMessage.success('参数已创建')
    }
    dialogVisible.value = false
    await handleSearch()
  } catch (e) {
    console.error('[sysConfig] 保存参数失败：', e)
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除参数「${row.configName}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
    })
    await deleteConfig(row.id)
    ElMessage.success('参数已删除')
    await handleSearch()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') console.error('[sysConfig] 删除参数失败：', e)
  }
}

loadData()
</script>
