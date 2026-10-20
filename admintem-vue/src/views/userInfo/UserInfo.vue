<template>
  <!-- 用户管理：列表分页 + 条件查询 + 新增/编辑/删除/重置密码/分配角色 -->
  <el-card shadow="never">
    <TableToolbar
      kicker="SYSTEM · 用户"
      title="用户管理"
      desc="维护登录账号、启用状态与角色归属；每个操作按钮都与后端同一套权限标识绑定，无权限即不渲染。"
    >
      <el-input
        v-model="query.userName"
        placeholder="账号"
        clearable
        style="width: 160px"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="query.nickName"
        placeholder="昵称"
        clearable
        style="width: 160px"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="query.phone"
        placeholder="手机号"
        clearable
        style="width: 160px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="启用" :value="0" />
        <el-option label="禁用" :value="1" />
      </el-select>
      <template #actions>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button v-perm="'sysAuthUser:addSysAuthUser'" type="primary" plain @click="openAdd">
          新增用户
        </el-button>
      </template>
    </TableToolbar>

    <!-- 列头排序：sortable="custom" 交给后端排序（字段名经白名单校验） -->
    <el-table v-loading="loading" :data="list" stripe border @sort-change="handleSortChange">
      <el-table-column prop="userName" label="账号" min-width="120" sortable="custom" />
      <el-table-column prop="nickName" label="昵称" min-width="120" sortable="custom" />
      <el-table-column prop="email" label="邮箱" min-width="160" sortable="custom" />
      <el-table-column prop="phone" label="手机号" width="120" />
      <el-table-column prop="deptName" label="部门" width="120">
        <template #default="{ row }">{{ row.deptName || "—" }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色" min-width="150">
        <template #default="{ row }">
          <el-tag
            v-for="name in row.roleNames"
            :key="name"
            size="small"
            type="info"
            style="margin-right: 4px"
          >
            {{ name }}
          </el-tag>
          <span v-if="!row.roleNames || !row.roleNames.length" class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button v-perm="'sysAuthUser:editSysAuthUser'" link type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-perm="'sysAuthUser:assignRole'" link type="warning" @click="openAssign(row)">
            分配角色
          </el-button>
          <el-button v-perm="'sysAuthUser:resetPassword'" link type="primary" @click="openReset(row)">
            重置密码
          </el-button>
          <el-button v-perm="'sysAuthUser:deleteById'" link type="danger" @click="handleDelete(row)">
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" append-to-body>
      <p class="dialog-desc">
        账号是登录标识，创建后不可修改；初始密码仅新增时必填；角色可在此一并分配，保存为全量覆盖。
      </p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="账号" prop="userName">
              <el-input v-model="form.userName" :disabled="!!form.id" placeholder="登录账号（不可修改）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="!form.id" label="初始密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="6~32 位" />
            </el-form-item>
            <el-form-item v-else label="新密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="留空表示不修改" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="0">启用</el-radio>
                <el-radio :value="1">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.sex">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-tree-select
                v-model="form.deptId"
                :data="deptOptions"
                :props="{ label: 'deptName', value: 'id', children: 'children' }"
                node-key="id"
                check-strictly
                clearable
                :render-after-expand="false"
                placeholder="不选表示未分配"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
                <el-option
                  v-for="r in roleOptions"
                  :key="r.id"
                  :label="r.roleName"
                  :value="r.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="handleCancelDialog">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保 存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="460px" append-to-body>
      <p class="dialog-desc">
        管理员为该用户设置新密码；重置成功后其旧令牌立即失效，需要重新登录。
      </p>
      <el-form ref="resetFormRef" :model="resetForm" label-width="90px">
        <el-form-item
          label="新密码"
          prop="newPassword"
          :rules="[
            { required: true, message: '请输入新密码', trigger: 'blur' },
            { min: 6, max: 32, message: '密码长度 6~32 位', trigger: 'blur' },
          ]"
        >
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="6~32 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitReset">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色弹窗 -->
    <el-dialog v-model="assignVisible" title="分配角色" width="520px" append-to-body>
      <p class="dialog-desc">
        保存为全量覆盖（先清空旧关联再写入）；内置管理员必须保留 admin 角色，移除会被拒绝。
      </p>
      <el-form label-width="90px">
        <el-form-item label="用户">
          <span>{{ assignUserName }}</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-select
            v-model="assignForm.roleIds"
            multiple
            placeholder="请选择角色（可多选）"
            style="width: 100%"
            :loading="assignLoading"
          >
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitAssign">保 存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
/**
 * 用户管理页面
 * - 列表分页 + 条件查询
 * - 新增/编辑/删除/重置密码/分配角色
 * - 各操作按钮均受 v-perm 指令控制（按钮级权限）
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as userApi from '@/api/user'
import * as deptApi from '@/api/dept'
import { queryAllRoles } from '@/api/role'
import TableToolbar from '@/components/TableToolbar.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useQuerySync } from '@/composables/useQuerySync'

const loading = ref(false)
const list = ref([])
const total = ref(0)

// 查询条件
// sortField/sortOrder：由表格列头排序事件写入，后端按白名单映射为真实列名（未登记字段会被忽略）
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  userName: '',
  nickName: '',
  phone: '',
  status: null,
  sortField: '',
  sortOrder: '',
})

// 部门树（用户归属；未分配可为空）
const deptOptions = ref([])

// 角色下拉选项
const roleOptions = ref([])

/** 加载用户列表（失败时由拦截器提示，此处仅保证 loading 收尾与留痕） */
async function loadData() {
  loading.value = true
  try {
    const res = await userApi.pageUsers({ ...query })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    // 失败时保留上一次列表数据，避免误判为“没有数据”
    console.error('[userInfo] 加载用户列表失败：', e)
  } finally {
    loading.value = false
  }
}

/** 分页切换（页码或每页条数变化） */
function onPageChange(page, size) {
  query.pageNum = page
  query.pageSize = size
  loadData()
}

/** 查询：回到第一页 */
function handleSearch() {
  query.pageNum = 1
  loadData()
}

/**
 * 列头排序：Element Plus 回调参数为 { prop, order }，order 为 asc/desc/null
 * 只把逻辑字段名交给后端，由后端白名单映射为列名（前端不接触真实列名）
 */
function handleSortChange({ prop, order }) {
  query.sortField = order ? prop : ''
  query.sortOrder = order === 'descending' ? 'desc' : order === 'ascending' ? 'asc' : ''
  query.pageNum = 1
  loadData()
}

/** 重置查询条件 */
function handleReset() {
  query.userName = ''
  query.nickName = ''
  query.phone = ''
  query.status = null
  handleSearch()
}

/** 加载角色选项（无权限查看角色时保持空列表，但需留痕便于排查） */
async function loadRoles() {
  try {
    const res = await queryAllRoles()
    roleOptions.value = res.data || []
  } catch (e) {
    roleOptions.value = []
    console.error('[userInfo] 加载角色下拉选项失败（列表仍可正常使用）：', e)
  }
}

/* ------------------------------ 新增/编辑 ------------------------------ */
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  userName: '',
  nickName: '',
  password: '',
  email: '',
  phone: '',
  sex: 1,
  deptId: null,
  status: 0,
  introduce: '',
  roleIds: [],
})

const rules = {
  userName: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,32}$/, message: '4~32 位字母、数字或下划线', trigger: 'blur' },
  ],
  nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  // 密码必填性随“新增/编辑”切换（见 openAdd / openEdit）
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6~32 位', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
}

/**
 * 加载部门树（用户归属选择用）。
 *
 * 失败只记日志、不弹错：部门选择是"可选增强"，
 * 即使接口失败也不应阻塞用户的新增/编辑（此时 deptId 保持为空）。
 */
async function loadDepts() {
  try {
    const res = await deptApi.treeList()
    deptOptions.value = res.data || []
  } catch (e) {
    console.error('[userInfo] 加载部门树失败：', e)
  }
}
/** 打开新增弹窗 */
function openAdd() {
  loadDepts()
  dialogTitle.value = '新增用户'
  // 恢复密码必填：若上次编辑弹窗是通过右上角关闭的，handleCancelDialog 不会执行
  rules.password[0].required = true
  Object.assign(form, {
    id: null,
    userName: '',
    nickName: '',
    password: '',
    email: '',
    phone: '',
    sex: 1,
    deptId: null,
    status: 0,
    introduce: '',
    roleIds: [],
  })
  dialogVisible.value = true
}

/** 打开编辑弹窗（编辑时密码留空表示不修改） */
async function openEdit(row) {
  loadDepts()
  dialogTitle.value = '编辑用户'
  Object.assign(form, {
    id: row.id,
    userName: row.userName,
    nickName: row.nickName,
    password: '',
    email: row.email,
    phone: row.phone,
    sex: row.sex ?? 1,
    deptId: row.deptId ?? null,
    status: row.status ?? 0,
    introduce: row.introduce,
    roleIds: [...(row.roleIds || [])],
  })
  // 编辑时不强制要求密码
  rules.password[0].required = false
  dialogVisible.value = true
}

/** 取消弹窗：恢复密码必填规则 */
function handleCancelDialog() {
  dialogVisible.value = false
  rules.password[0].required = true
}

/** 保存新增/编辑 */
async function saveForm() {
  // 校验失败时 Element Plus 会 reject，此处以返回值判断，避免未处理的 Promise rejection
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (saving.value) return
  saving.value = true
  try {
    const payload = { ...form }
    if (form.id) {
      // 编辑时密码为空则删除该字段，避免触发后端校验
      if (!payload.password) delete payload.password
      await userApi.editUser(payload)
      ElMessage.success('用户信息已更新')
    } else {
      await userApi.addUser(payload)
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    // 等待列表刷新结束再收尾 loading，避免保存按钮与列表 loading 状态错位
    await handleSearch()
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出，此处仅留痕
    console.error('[userInfo] 保存用户失败：', e)
  } finally {
    saving.value = false
  }
}

/* ------------------------------ 删除 ------------------------------ */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.userName}」吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    // 用户取消确认：ElMessageBox 以 reject 结束，属正常流程
    return
  }
  try {
    await userApi.deleteUser(row.id)
    ElMessage.success('删除成功')
    // 删除的是当前页最后一条时回退一页，否则停留在已空的超界页
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    await loadData()
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出，此处仅留痕
    console.error('[userInfo] 删除用户失败：', e)
  }
}

/* ------------------------------ 重置密码 ------------------------------ */
const resetVisible = ref(false)
const resetForm = reactive({ userId: null, newPassword: '' })
const resetFormRef = ref()

function openReset(row) {
  resetForm.userId = row.id
  resetForm.newPassword = ''
  resetVisible.value = true
}

async function submitReset() {
  // 校验失败时 Element Plus 会 reject，此处以返回值判断，避免未处理的 Promise rejection
  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await userApi.resetUserPassword({ ...resetForm })
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出，此处仅留痕
    console.error('[userInfo] 重置密码失败：', e)
  }
}

/* ------------------------------ 分配角色 ------------------------------ */
const assignVisible = ref(false)
const assignForm = reactive({ userId: null, roleIds: [] })
const assignUserName = ref('')
const assignLoading = ref(false)

async function openAssign(row) {
  assignForm.userId = row.id
  assignForm.roleIds = []
  assignUserName.value = `${row.userName}（${row.nickName || '-'}）`
  assignLoading.value = true
  assignVisible.value = true
  try {
    const res = await userApi.getUserRoles(row.id)
    assignForm.roleIds = res.data.roleIds || []
  } catch (e) {
    // 加载已分配角色失败：保持空选择，仅留痕避免误判为“未分配角色”
    console.error('[userInfo] 加载用户已分配角色失败：', e)
  } finally {
    assignLoading.value = false
  }
}

async function submitAssign() {
  if (assignLoading.value) return
  try {
    await userApi.assignUserRoles({ userId: assignForm.userId, roleIds: assignForm.roleIds })
    ElMessage.success('角色分配成功')
    assignVisible.value = false
    await loadData()
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出，此处仅留痕
    console.error('[userInfo] 分配角色失败：', e)
  }
}

/* ------------------------------ 展示辅助 ------------------------------ */
const statusText = (v) => (v === 1 ? '禁用' : '启用')
const statusType = (v) => (v === 1 ? 'danger' : 'success')

// 查询条件与 URL 双向可读（刷新/分享/前进后退都不丢筛选与页码）
// 注意：onHydrated 里先加载列表——此时 query 已合并 URL 上的条件
useQuerySync(query, { onHydrated: loadData })

onMounted(() => {
  loadRoles()
})
</script>
