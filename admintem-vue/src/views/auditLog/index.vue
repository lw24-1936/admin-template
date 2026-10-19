<template>
  <!--
    操作审计：只读列表。
    数据由后端切面异步写入（登录、增删改、越权被拒等），这里只负责查询与展示。
    页面入口权限：菜单 auditLog:auditLog；查询接口权限：sysAuthAuditLog:paginQuery
  -->
  <el-card shadow="never">
    <TableToolbar
      kicker="SYSTEM · 审计"
      title="操作审计"
      desc="一次操作一行：谁、在什么时候、对什么做了什么、成功还是被拒绝。数据只追加、不在页面修改，可据此追溯权限变更历史；默认按发生时间倒序。"
    >
      <el-input
        v-model="query.operator"
        placeholder="操作人账号"
        clearable
        style="width: 150px"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="query.action"
        placeholder="动作"
        clearable
        filterable
        style="width: 170px"
        @change="handleSearch"
      >
        <el-option
          v-for="item in actionOptions"
          :key="item.action"
          :label="`${item.actionName || item.action}（${item.total}）`"
          :value="item.action"
        />
      </el-select>
      <el-input
        v-model="query.targetName"
        placeholder="目标名称"
        clearable
        style="width: 150px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.result" placeholder="结果" clearable style="width: 110px" @change="handleSearch">
        <el-option label="成功" :value="0" />
        <el-option label="失败" :value="1" />
      </el-select>
      <el-date-picker
        v-model="timeRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="YYYY-MM-DD HH:mm:ss"
        style="width: 360px"
        @change="handleSearch"
      />
      <template #actions>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </template>
    </TableToolbar>

    <el-table v-loading="loading" :data="list" stripe border>
      <el-table-column prop="createdTime" label="发生时间" width="170" />
      <el-table-column prop="operator" label="操作人" width="120">
        <template #default="{ row }">
          {{ row.operator || '（匿名）' }}
        </template>
      </el-table-column>
      <el-table-column label="动作" min-width="150">
        <template #default="{ row }">
          <span>{{ row.actionName || row.action }}</span>
          <el-tag v-if="row.actionName && row.actionName !== row.action" size="small" type="info" effect="plain"
                  style="margin-left: 6px">{{ row.action }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="目标" min-width="170">
        <template #default="{ row }">
          <span v-if="row.targetName">{{ row.targetName }}</span>
          <span v-else class="audit-muted">—</span>
          <el-tag v-if="row.targetType" size="small" effect="plain" style="margin-left: 6px">
            {{ row.targetType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="90">
        <template #default="{ row }">
          <el-tag :type="row.result === 0 ? 'success' : 'danger'" size="small">
            {{ row.resultText }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="costMs" label="耗时" width="90">
        <template #default="{ row }">
          <span v-if="row.costMs != null">{{ row.costMs }} ms</span>
          <span v-else class="audit-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="clientIp" label="来源IP" width="130">
        <template #default="{ row }">{{ row.clientIp || '—' }}</template>
      </el-table-column>
      <el-table-column label="详情" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <PaginationBar
      :total="total"
      :page="query.pageNum"
      :size="query.pageSize"
      @change="onPageChange"
    />

    <!-- 明细抽屉：把不适合放在列表里的长字段（请求路径、失败原因、traceId）集中展示 -->
    <el-drawer v-model="detailVisible" title="审计明细" size="480px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="发生时间">{{ current.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ current.operator || '（匿名）' }}<span v-if="current.operatorId">（ID {{ current.operatorId }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="动作">
          {{ current.actionName || '-' }}<span class="audit-muted"> / {{ current.action }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="目标">
          {{ current.targetName || '-' }}
          <span v-if="current.targetType" class="audit-muted">（{{ current.targetType }}<span
            v-if="current.targetId"> #{{ current.targetId }}</span>）</span>
        </el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag :type="current.result === 0 ? 'success' : 'danger'" size="small">{{ current.resultText }}</el-tag>
          <span v-if="current.errorCode" class="audit-muted"> 错误码 {{ current.errorCode }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="current.errorMsg" label="失败原因">
          <span class="audit-error">{{ current.errorMsg }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="请求">{{ current.httpMethod }} {{ current.requestUri }}</el-descriptions-item>
        <el-descriptions-item label="来源IP">{{ current.clientIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ current.costMs != null ? current.costMs + ' ms' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="链路ID">
          <code class="audit-trace">{{ current.traceId || '-' }}</code>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </el-card>
</template>

<script setup>
/**
 * 操作审计页面。
 *
 * 三条约定：
 *   1. 只读：页面不提供编辑/删除入口（审计记录只追加，清理走运维 SQL）；
 *   2. 排序固定：后端固定"发生时间倒序"，前端不提供列头排序，避免与索引策略打架；
 *   3. URL 同步：筛选条件与页码写回地址栏，刷新/分享后不丢失（useQuerySync）。
 */
import { onMounted, reactive, ref } from 'vue'
import * as auditApi from '@/api/audit'
import TableToolbar from '@/components/TableToolbar.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useQuerySync } from '@/composables/useQuerySync'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const actionOptions = ref([])

// 时间范围：'YYYY-MM-DD HH:mm:ss' 两元组，与后端约定的格式一致
const timeRange = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  operator: '',
  action: '',
  targetName: '',
  result: null,
  beginTime: '',
  endTime: '',
})

const detailVisible = ref(false)
const current = ref(null)

/** 加载列表（失败由请求拦截器统一提示，这里只收尾 loading） */
async function loadData() {
  loading.value = true
  try {
    // 空字符串不传给后端：避免把 "" 当成过滤条件（SQL 里会变成 LIKE '%'）
    const payload = { ...query }
    Object.keys(payload).forEach((k) => {
      if (payload[k] === '' || payload[k] === null) delete payload[k]
    })
    const res = await auditApi.pageAuditLogs(payload)
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error('[auditLog] 加载审计列表失败：', e)
  } finally {
    loading.value = false
  }
}

/** 动作字典（失败不影响列表，只是下拉为空） */
async function loadActions() {
  try {
    const res = await auditApi.queryActionDict()
    actionOptions.value = res.data || []
  } catch (e) {
    console.error('[auditLog] 加载动作字典失败：', e)
  }
}

/** 分页变化 */
function onPageChange(page, size) {
  query.pageNum = page
  query.pageSize = size
  loadData()
}

/** 查询：回到第一页，并把时间范围拆成两个字段 */
function handleSearch() {
  query.pageNum = 1
  query.beginTime = timeRange.value && timeRange.value[0] ? timeRange.value[0] : ''
  query.endTime = timeRange.value && timeRange.value[1] ? timeRange.value[1] : ''
  loadData()
}

/** 重置：清空所有条件与时间范围 */
function handleReset() {
  query.operator = ''
  query.action = ''
  query.targetName = ''
  query.result = null
  query.beginTime = ''
  query.endTime = ''
  timeRange.value = []
  query.pageNum = 1
  query.pageSize = 10
  loadData()
}

/** 打开明细抽屉 */
function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

// 首屏：水合 URL 上的条件（若有）后加载；动作字典并行加载
useQuerySync(query, { onHydrated: loadData })
onMounted(() => {
  // 从 URL 还原时间范围，保证刷新后日期控件也回显
  if (query.beginTime || query.endTime) {
    timeRange.value = [query.beginTime || '', query.endTime || '']
  }
  loadActions()
})
</script>

<style scoped>
.audit-muted {
  color: var(--lc-ink-faint);
}
.audit-error {
  color: var(--lc-danger);
  word-break: break-all;
}
.audit-trace {
  font-family: var(--lc-font-mono);
  font-size: var(--lc-fs-hint);
  word-break: break-all;
}
</style>
