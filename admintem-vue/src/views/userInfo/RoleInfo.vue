<template>
  <!-- 角色管理：列表分页 + 条件查询 + 新增/编辑/删除 + 树形分配权限 -->
  <el-card shadow="never">
    <TableToolbar
      kicker="SYSTEM · 角色"
      title="角色管理"
      desc="角色决定可访问的菜单与可执行的操作；内置超级角色受保护，仍绑定用户的角色需先解绑才能删除。"
    >
      <el-input
        v-model="query.roleName"
        placeholder="角色名称"
        clearable
        style="width: 160px"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="query.roleKey"
        placeholder="角色标识"
        clearable
        style="width: 160px"
        @keyup.enter="handleSearch"
      />
      <template #actions>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button v-perm="'sysAuthRole:addAuthRole'" type="primary" plain @click="openAdd">
          新增角色
        </el-button>
      </template>
    </TableToolbar>

    <!-- 列头排序：sortable="custom" 交给后端排序（字段名经白名单校验） -->
    <el-table v-loading="loading" :data="list" stripe border @sort-change="handleSortChange">
      <el-table-column prop="id" label="ID" width="70" sortable="custom" />
      <el-table-column prop="roleName" label="角色名称" min-width="140" sortable="custom" />
      <el-table-column prop="roleKey" label="角色标识" min-width="140" sortable="custom">
        <template #default="{ row }">
          <el-tag v-if="isBuiltIn(row)" type="danger" size="small">内置</el-tag>
          <span style="margin-left: 4px">{{ row.roleKey }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="dataScope" label="数据范围" min-width="130">
        <template #default="{ row }">
          {{ scopeLabel(row.dataScope) }}
        </template>
      </el-table-column>
      <el-table-column prop="createdBy" label="创建人" width="120" />
      <el-table-column prop="createdTime" label="创建时间" width="170" sortable="custom" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-perm="'sysAuthRole:assignPermission'" link type="primary" @click="openPerm(row)">
            分配权限
          </el-button>
          <el-button
            v-perm="'sysAuthRole:editAuthRole'"
            link
            type="warning"
            :disabled="isBuiltIn(row)"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            v-perm="'sysAuthRole:deleteById'"
            link
            type="danger"
            :disabled="isBuiltIn(row)"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 公共分页条 -->
    <PaginationBar
      :total="total"
      :page="query.pageNum"
      :size="query.pageSize"
      @change="onPageChange"
    />

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px" append-to-body>
      <p class="dialog-desc">
        角色标识 role_key 全局唯一（小写字母开头，2~32 位）；保存后可在「分配权限」中勾选菜单与按钮。
      </p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如：运营专员" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="form.roleKey" :disabled="editingBuiltIn" placeholder="如：operator" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option v-for="item in scopeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保 存</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限弹窗 -->
    <el-dialog
      v-model="permVisible"
      :title="`分配权限 - ${currentRole.name}`"
      width="560px"
      append-to-body
    >
      <p class="dialog-desc">
        按树勾选：菜单权限控制页面是否可见，按钮权限控制页面内操作是否可用；
        保存为全量覆盖，该角色下的用户下次请求即生效新权限。
      </p>
      <el-tree
        ref="treeRef"
        :data="treeData"
        :props="treeProps"
        node-key="id"
        show-checkbox
        default-expand-all
        :expand-on-click-node="false"
        v-loading="permLoading"
        style="max-height: 420px; overflow-y: auto"
      >
        <template #default="{ data }">
          <span>
            {{ data.name }}
            <el-tag v-if="data.type === 1" size="small" type="success" style="margin-left: 6px">按钮</el-tag>
            <el-tag v-else size="small" type="info" style="margin-left: 6px">菜单</el-tag>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="permVisible = false">取 消</el-button>
        <el-button type="primary" :loading="permSaving" @click="submitPerm">保 存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
/**
 * 角色管理页面
 * - 列表分页 + 条件查询
 * - 新增/编辑/删除角色
 * - 权限分配（树形勾选：菜单 + 按钮）
 * - 操作按钮由 v-perm 指令控制
 */
import { nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as roleApi from '@/api/role'
import TableToolbar from '@/components/TableToolbar.vue'
import { useQuerySync } from '@/composables/useQuerySync'
import PaginationBar from '@/components/PaginationBar.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  roleName: '',
  roleKey: '',
  // 列头排序：仅传逻辑字段名与方向，后端按白名单映射为列名
  sortField: '',
  sortOrder: '',
})

/** 加载角色列表（失败时由拦截器提示，此处仅保证 loading 收尾与留痕） */
async function loadData() {
  loading.value = true
  try {
    const res = await roleApi.pageRoles({ ...query })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error('[roleInfo] 加载角色列表失败：', e)
  } finally {
    loading.value = false
  }
}

/** 分页切换 */
function onPageChange(page, size) {
  query.pageNum = page
  query.pageSize = size
  loadData()
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

/**
 * 列头排序：Element Plus 回调参数为 { prop, order }，order 为 asc/desc/null
 * 仅把逻辑字段名交给后端，由后端白名单映射为列名（未登记字段会被忽略并回落默认排序）
 */
function handleSortChange({ prop, order }) {
  query.sortField = order ? prop : ''
  query.sortOrder = order === 'descending' ? 'desc' : order === 'ascending' ? 'asc' : ''
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.roleName = ''
  query.roleKey = ''
  handleSearch()
}

/* ------------------------------ 新增/编辑 ------------------------------ */
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const saving = ref(false)
const editingBuiltIn = ref(false)
const formRef = ref()
const form = reactive({ id: null, roleName: '', roleKey: '', dataScope: 'ALL' })

const scopeOptions = [
  { value: 'ALL', label: '全部数据' },
  { value: 'DEPT_AND_CHILD', label: '本部门及子部门' },
  { value: 'DEPT', label: '本部门' },
  { value: 'SELF', label: '仅本人' },
]

function scopeLabel(value) {
  return scopeOptions.find((item) => item.value === value)?.label || '全部数据'
}

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [
    { required: true, message: '请输入角色标识', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]{1,31}$/, message: '小写字母开头，可含数字与下划线', trigger: 'blur' },
  ],
}

function openAdd() {
  dialogTitle.value = '新增角色'
  editingBuiltIn.value = false
  Object.assign(form, { id: null, roleName: '', roleKey: '', dataScope: 'ALL' })
  dialogVisible.value = true
}

function openEdit(row) {
  dialogTitle.value = '编辑角色'
  editingBuiltIn.value = isBuiltIn(row)
  Object.assign(form, { id: row.id, roleName: row.roleName, roleKey: row.roleKey, dataScope: row.dataScope || 'ALL' })
  dialogVisible.value = true
}

async function saveForm() {
  // 校验失败时 Element Plus 会 reject，此处以返回值判断，避免未处理的 Promise rejection
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (saving.value) return
  saving.value = true
  try {
    const payload = { ...form }
    if (form.id) {
      await roleApi.editRole(payload)
      ElMessage.success('角色信息已更新')
    } else {
      await roleApi.addRole(payload)
      ElMessage.success('角色创建成功')
    }
    dialogVisible.value = false
    // 等待列表刷新结束再收尾 loading，避免保存按钮与列表 loading 状态错位
    await handleSearch()
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出，此处仅留痕
    console.error('[roleInfo] 保存角色失败：', e)
  } finally {
    saving.value = false
  }
}

/* ------------------------------ 删除 ------------------------------ */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    // 用户取消确认：ElMessageBox 以 reject 结束，属正常流程
    return
  }
  try {
    await roleApi.deleteRole(row.id)
    ElMessage.success('删除成功')
    // 删除的是当前页最后一条时回退一页，否则停留在已空的超界页
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    await loadData()
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出，此处仅留痕
    console.error('[roleInfo] 删除角色失败：', e)
  }
}

/* ------------------------------ 分配权限（树形） ------------------------------ */
const permVisible = ref(false)
const permLoading = ref(false)
const permSaving = ref(false)
const treeRef = ref()
const treeData = ref([])
const treeProps = { label: 'name', children: 'children' }
const currentRole = reactive({ id: null, name: '' })

/** 内置 admin 角色标识（后端也不允许修改/删除） */
const isBuiltIn = (row) => row.roleKey === 'admin'

async function openPerm(row) {
  currentRole.id = row.id
  currentRole.name = row.roleName
  permVisible.value = true
  permLoading.value = true
  try {
    const res = await roleApi.getRolePermissions(row.id)
    treeData.value = res.data.permissionTree || []
    await nextTick()
    // 回显已勾选权限（含父级与按钮）；treeRef 在弹窗渲染完成后才存在，此处做空值保护
    if (treeRef.value) {
      treeRef.value.setCheckedKeys(res.data.permissionIds || [])
    }
  } catch (e) {
    // 加载权限树失败：清空树，避免展示上一个角色残留的勾选状态
    treeData.value = []
    console.error('[roleInfo] 加载角色权限失败：', e)
  } finally {
    permLoading.value = false
  }
}

/** 保存授权：勾选节点 + 半选（父级菜单）一起提交，保证父子权限链路完整 */
async function submitPerm() {
  if (!treeRef.value) return
  if (permSaving.value) return
  permSaving.value = true
  try {
    const checked = treeRef.value.getCheckedKeys() || []
    const half = treeRef.value.getHalfCheckedKeys() || []
    await roleApi.assignRolePermissions({
      roleId: currentRole.id,
      permissionIds: [...new Set([...checked, ...half])],
    })
    ElMessage.success('权限分配成功')
    permVisible.value = false
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出，此处仅留痕
    console.error('[roleInfo] 分配权限失败：', e)
  } finally {
    permSaving.value = false
  }
}

// 查询条件与 URL 同步（刷新/分享/前进后退都不丢筛选与页码）
useQuerySync(query, { onHydrated: loadData })
</script>
