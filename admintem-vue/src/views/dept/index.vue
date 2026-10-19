<template>
  <!-- 部门管理：树形表格 + 新增/编辑/删除（存在子部门或用户时禁止删除） -->
  <el-card shadow="never">
    <TableToolbar
      kicker="SYSTEM · 组织"
      title="部门管理"
      desc="部门只决定“人归属于哪里”，本身不承载权限；用户可归属一个部门。删除前必须先清空子部门与部门下用户，避免出现挂在不存在的部门上的脏数据。"
    >
      <template #actions>
        <el-button type="primary" @click="loadData">刷新</el-button>
        <el-button v-perm="'sysAuthDept:addDept'" type="primary" plain @click="openAdd(null)">
          新增顶级部门
        </el-button>
      </template>
    </TableToolbar>

    <el-table
      v-loading="loading"
      :data="tree"
      row-key="id"
      default-expand-all
      :tree-props="{ children: 'children' }"
      stripe
      border
    >
      <el-table-column prop="deptName" label="部门名称" min-width="200" />
      <el-table-column prop="leader" label="负责人" width="140">
        <template #default="{ row }">{{ row.leader || '—' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="联系电话" width="150">
        <template #default="{ row }">{{ row.phone || '—' }}</template>
      </el-table-column>
      <el-table-column label="人数" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.userCount > 0" size="small" type="info" effect="plain">{{ row.userCount }}</el-tag>
          <span v-else class="dept-muted">0</span>
        </template>
      </el-table-column>
      <el-table-column prop="orderNum" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-perm="'sysAuthDept:addDept'" link type="primary" @click="openAdd(row)">新增子部门</el-button>
          <el-button v-perm="'sysAuthDept:editDept'" link type="warning" @click="openEdit(row)">编辑</el-button>
          <el-button v-perm="'sysAuthDept:deleteById'" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹层 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑部门' : '新增部门'" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            node-key="id"
            check-strictly
            :render-after-expand="false"
            placeholder="选择上级部门（顶级选择“无”）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" maxlength="64" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" maxlength="32" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" maxlength="20" placeholder="如 010-00000001" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.orderNum" :min="1" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">启用</el-radio>
            <el-radio :value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
/**
 * 部门管理页面。
 *
 * 三点说明：
 *   1. 树形表格：后端一次返回全量树（部门数量很小），前端直接展开，不做懒加载；
 *   2. 上级选择禁止选自己/自己的下级：前端把候选集裁掉，后端也会再校验（双保险）；
 *   3. 删除是逻辑删除，但会先检查"有没有子部门/有没有用户"，由后端给出具体原因。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as deptApi from '@/api/dept'
import TableToolbar from '@/components/TableToolbar.vue'

const loading = ref(false)
const saving = ref(false)
const tree = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  parentId: 0,
  deptName: '',
  leader: '',
  phone: '',
  orderNum: 1,
  status: 0,
})

const rules = {
  parentId: [{ required: true, message: '请选择上级部门（顶级选“无”）', trigger: 'change' }],
  deptName: [
    { required: true, message: '请输入部门名称', trigger: 'blur' },
    { max: 64, message: '长度不能超过 64', trigger: 'blur' },
  ],
  phone: [{ pattern: /^$|^[0-9-+() ]{6,20}$/, message: '电话格式不正确', trigger: 'blur' }],
}

/**
 * 上级部门候选：顶级(0) + 整棵树；编辑时排除自己与自己的子孙（避免成环）。
 */
const parentOptions = computed(() => {
  const root = { id: 0, deptName: '无（作为顶级部门）', children: [] }
  const prune = (nodes) => {
    const result = []
    for (const node of nodes) {
      if (form.id && node.id === form.id) continue // 跳过自己（其子树自然也不会进来）
      result.push({
        id: node.id,
        deptName: node.deptName,
        children: prune(node.children || []),
      })
    }
    return result
  }
  root.children = prune(tree.value)
  return [root]
})

/** 加载部门树 */
async function loadData() {
  loading.value = true
  try {
    const res = await deptApi.treeList()
    tree.value = res.data || []
  } catch (e) {
    console.error('[dept] 加载部门树失败：', e)
  } finally {
    loading.value = false
  }
}

/** 新增（parent 为空表示顶级） */
function openAdd(parent) {
  Object.assign(form, {
    id: null,
    parentId: parent ? parent.id : 0,
    deptName: '',
    leader: '',
    phone: '',
    orderNum: 1,
    status: 0,
  })
  dialogVisible.value = true
}

/** 编辑 */
function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId ?? 0,
    deptName: row.deptName,
    leader: row.leader || '',
    phone: row.phone || '',
    orderNum: row.orderNum ?? 1,
    status: row.status ?? 0,
  })
  dialogVisible.value = true
}

/** 保存 */
async function handleSave() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  saving.value = true
  try {
    if (form.id) {
      await deptApi.editDept({ ...form })
      ElMessage.success('已保存')
    } else {
      await deptApi.addDept({ ...form })
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    // 业务错误（名称重复、上级非法等）由拦截器统一提示
    console.error('[dept] 保存部门失败：', e)
  } finally {
    saving.value = false
  }
}

/** 删除（后端会拒绝删除有子部门/有用户的部门） */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除部门「${row.deptName}」吗？存在子部门或该部门下仍有用户时会被拒绝。`,
      '删除部门',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deptApi.deleteDept(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    console.error('[dept] 删除部门失败：', e)
  }
}

onMounted(loadData)
</script>

<style scoped>
.dept-muted {
  color: var(--lc-ink-faint);
}
</style>
