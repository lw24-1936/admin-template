<template>
  <!-- 权限管理：菜单/按钮两级树形维护 -->
  <el-card shadow="never">
    <TableToolbar
      kicker="SYSTEM · 权限"
      title="权限管理"
      desc="菜单（type=0）控页面是否可见、按钮（type=1）控操作是否可用；禁用即不可访问，隐藏仍可直达 URL。"
    >
      <template #actions>
        <el-button v-perm="'sysAuthPermission:addAuthPermission'" type="primary" plain @click="openAdd(null)">
          新增顶级权限
        </el-button>
      </template>
    </TableToolbar>

    <el-table
      v-loading="loading"
      :data="treeData"
      row-key="id"
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column prop="name" label="权限名称" min-width="180" />
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="permissionKey" label="权限标识" min-width="180" show-overflow-tooltip />
      <el-table-column prop="backendUrl" label="后端接口" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.backendUrl" class="text-muted">{{ row.backendUrl }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="menuUrl" label="路由/组件" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.type === 0" class="text-muted">{{ row.menuUrl }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderNum" label="排序" width="70" align="center" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-perm="'sysAuthPermission:addAuthPermission'" link type="primary" @click="openAdd(row)">
            新增子级
          </el-button>
          <el-button v-perm="'sysAuthPermission:editAuthPermission'" link type="warning" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-perm="'sysAuthPermission:deleteById'" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" append-to-body>
      <p class="dialog-desc">
        菜单类型填菜单路由与组件路径（对应 src/views 下相对路径）；按钮类型填后端接口路径。
        权限标识 permission_key 前后端共用，修改会影响所有已授权角色。
      </p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="权限类型">
              <el-radio-group v-model="form.type">
                <el-radio :value="0">菜单</el-radio>
                <el-radio :value="1">按钮（操作）</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上级">
              <el-tree-select
                v-model="form.parentId"
                :data="parentOptions"
                :props="{ label: 'name', value: 'id' }"
                node-key="id"
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限名称" prop="name">
              <el-input v-model="form.name" placeholder="如：新增用户" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限标识" prop="permissionKey">
              <el-input v-model="form.permissionKey" placeholder="如：sysAuthUser:addSysAuthUser" />
            </el-form-item>
          </el-col>

          <!-- 菜单类型专属字段 -->
          <template v-if="form.type === 0">
            <el-col :span="12">
              <el-form-item label="菜单路由" prop="menuUrl">
                <el-input v-model="form.menuUrl" placeholder="如：/userInfo" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="组件路径" prop="component">
                <el-input v-model="form.component" placeholder="如：/userInfo/UserInfo" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="图标">
                <el-input v-model="form.icon" placeholder="Element Plus 图标名，如 User" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="展示状态">
                <el-radio-group v-model="form.show">
                  <el-radio :value="0">展示</el-radio>
                  <el-radio :value="1">隐藏</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </template>

          <!-- 按钮类型专属字段 -->
          <template v-else>
            <el-col :span="12">
              <el-form-item label="后端接口" prop="backendUrl">
                <el-input v-model="form.backendUrl" placeholder="如：/sysAuthUser/addSysAuthUser" />
              </el-form-item>
            </el-col>
          </template>

          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="0">启用</el-radio>
                <el-radio :value="1">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.orderNum" :min="0" :max="999" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保 存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
/**
 * 权限管理页面（菜单 + 按钮树形维护）
 * - 树表展示（菜单为父节点，按钮为子节点）
 * - 新增（子级）/编辑/删除
 * - 新增与编辑表单按类型展示不同字段
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as permApi from '@/api/permission'
import TableToolbar from '@/components/TableToolbar.vue'

const loading = ref(false)
const treeData = ref([])

/** 加载权限树（失败时由拦截器提示，此处仅保证 loading 收尾与留痕） */
async function loadData() {
  loading.value = true
  try {
    const res = await permApi.treePermissions(true)
    treeData.value = res.data || []
  } catch (e) {
    console.error('[permissionInfo] 加载权限树失败：', e)
  } finally {
    loading.value = false
  }
}

/* ------------------------------ 新增/编辑 ------------------------------ */
const dialogVisible = ref(false)
const dialogTitle = ref('新增权限')
const saving = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  name: '',
  parentId: 0,
  type: 0, // 0 菜单 / 1 按钮
  orderNum: null,
  menuUrl: '',
  component: '',
  backendUrl: '',
  status: 0,
  show: 0,
  icon: '',
  permissionKey: '',
})

const rules = {
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  permissionKey: [{ required: true, message: '请输入权限标识', trigger: 'blur' }],
  // 菜单路由/组件仅“菜单”类型需要：字段在按钮类型下被 v-if 隐藏，
  // 若仍为必填，切换类型后将出现“校验失败但页面无任何提示”而无法保存
  menuUrl: [
    {
      validator: (rule, value, callback) => {
        if (form.type === 0 && !value) {
          callback(new Error('请输入菜单路由'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  component: [
    {
      validator: (rule, value, callback) => {
        if (form.type === 0 && !value) {
          callback(new Error('请输入组件路径'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

/** 父级选择：把现有树拍平成可选项（不含按钮类型作为父级） */
const parentOptions = ref([])
function flattenMenus(nodes, depth = 0) {
  const out = []
  nodes.forEach((n) => {
    if (n.type === 0) {
      out.push({ id: n.id, name: `${'　'.repeat(depth)}${n.name}` })
      if (n.children && n.children.length) {
        out.push(...flattenMenus(n.children, depth + 1))
      }
    }
  })
  return out
}

function openAdd(row) {
  dialogTitle.value = '新增权限'
  Object.assign(form, {
    id: null,
    name: '',
    parentId: row ? row.id : 0,
    type: row && row.type === 0 ? 1 : 0, // 在菜单下新增默认按钮，在根/按钮下新增默认菜单
    orderNum: null,
    menuUrl: '',
    component: '',
    backendUrl: '',
    status: 0,
    show: 0,
    icon: '',
    permissionKey: '',
  })
  parentOptions.value = [{ id: 0, name: '顶级菜单' }, ...flattenMenus(treeData.value)]
  dialogVisible.value = true
}

function openEdit(row) {
  dialogTitle.value = '编辑权限'
  Object.assign(form, {
    id: row.id,
    name: row.name,
    parentId: row.parentId,
    type: row.type,
    orderNum: row.orderNum,
    menuUrl: row.menuUrl || '',
    component: row.component || '',
    backendUrl: row.backendUrl || '',
    status: row.status ?? 0,
    show: row.show ?? 0,
    icon: row.icon || '',
    permissionKey: row.permissionKey || '',
  })
  parentOptions.value = [{ id: 0, name: '顶级菜单' }, ...flattenMenus(treeData.value)]
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
      await permApi.editPermission(payload)
      ElMessage.success('权限已更新')
    } else {
      await permApi.addPermission(payload)
      ElMessage.success('权限创建成功')
    }
    dialogVisible.value = false
    // 等待树刷新结束再收尾 loading，避免保存按钮与列表 loading 状态错位
    await loadData()
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出，此处仅留痕
    console.error('[permissionInfo] 保存权限失败：', e)
  } finally {
    saving.value = false
  }
}

/* ------------------------------ 删除 ------------------------------ */
async function handleDelete(row) {
  const tip =
    row.type === 0 && row.children && row.children.length
      ? `确定删除菜单「${row.name}」吗？其下子级（含按钮）将一并删除。`
      : `确定删除「${row.name}」吗？`
  try {
    await ElMessageBox.confirm(tip, '删除确认', { type: 'warning' })
  } catch {
    // 用户取消确认：ElMessageBox 以 reject 结束，属正常流程
    return
  }
  try {
    await permApi.deletePermission(row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出，此处仅留痕
    console.error('[permissionInfo] 删除权限失败：', e)
  }
}

/* ------------------------------ 展示辅助 ------------------------------ */
const typeText = (v) => (v === 1 ? '按钮' : '菜单')
const typeTag = (v) => (v === 1 ? 'success' : 'primary')
const statusText = (v) => (v === 1 ? '禁用' : '启用')
const statusTag = (v) => (v === 1 ? 'danger' : 'success')

onMounted(loadData)
</script>
