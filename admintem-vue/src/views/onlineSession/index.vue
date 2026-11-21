<template>
  <!--
    在线会话：查看当前持有有效令牌的账号，并可强制下线。
    "在线"的依据是 Redis 令牌白名单（admintem:auth:jwt:*），不是浏览器标签页是否打开。
  -->
  <el-card shadow="never">
    <TableToolbar
      kicker="SYSTEM · 会话"
      title="在线会话"
      desc="列表来自服务端令牌白名单：有未过期且未吊销的令牌即视为在线。强制下线会删除该账号的白名单，其下一次请求即被拒绝（无需等待令牌自然过期）。"
    >
      <el-input
        v-model="keyword"
        placeholder="按账号/昵称过滤"
        clearable
        style="width: 200px"
        @keyup.enter="loadData"
      />
      <template #actions>
        <el-button type="primary" @click="loadData">刷新</el-button>
        <el-button v-perm="'sysAuthSession:kick'" plain @click="handleKickAll">全部下线</el-button>
      </template>
    </TableToolbar>

    <el-table v-loading="loading" :data="paged" stripe border>
      <el-table-column label="账号" min-width="140">
        <template #default="{ row }">
          <span>{{ row.userName }}</span>
          <el-tag v-if="row.current" size="small" type="success" style="margin-left: 6px">当前</el-tag>
          <el-tag v-if="row.locked" size="small" type="warning" style="margin-left: 4px">锁定中</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="nickName" label="昵称" min-width="120">
        <template #default="{ row }">{{ row.nickName || '—' }}</template>
      </el-table-column>
        <el-table-column label="设备" min-width="150">
          <template #default="{ row }">
            <code class="session-digest">{{ row.deviceId || '旧版单会话' }}</code>
          </template>
        </el-table-column>
      <el-table-column prop="roleNames" label="角色" min-width="150">
        <template #default="{ row }">{{ row.roleNames || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="剩余有效期" width="130">
        <template #default="{ row }">{{ formatRemain(row.expireSeconds) }}</template>
      </el-table-column>
      <el-table-column prop="tokenDigest" label="令牌摘要" width="120">
        <template #default="{ row }">
          <code class="session-digest">{{ row.tokenDigest || '—' }}</code>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button
            v-perm="'sysAuthSession:kick'"
            link
            type="danger"
            :disabled="row.current"
            :title="row.current ? '当前登录会话请用「退出登录」结束' : ''"
            @click="handleKick(row)"
          >
            强制下线
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <p class="session-hint">
      共 {{ total }} 个在线会话（第 {{ page }} / {{ totalPages }} 页）；"令牌摘要"只显示令牌前 8 位，用于区分同一账号的多条会话，不泄露完整令牌。
    </p>

    <PaginationBar
      :total="total"
      :page="page"
      :size="pageSize"
      :page-sizes="[10, 20, 50]"
      @change="onPageChange"
    />
  </el-card>
</template>

<script setup>
/**
 * 在线会话页面。
 *
 * 三条约定：
 *   1. 不下发完整令牌：只给前 8 位摘要，避免页面被截图/日志采集时泄露可用凭证；
 *   2. 当前会话不允许自我强踢：按钮禁用，提示改用"退出登录"（自我强踢会让用户莫名其妙掉线）；
 *   3. 强制下线是幂等操作：目标已不在线也返回成功（语义是"操作后该账号已无有效令牌"）。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as sessionApi from '@/api/session'
import TableToolbar from '@/components/TableToolbar.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const loading = ref(false)
const list = ref([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)

/** 前端过滤（在线人数通常很少，无需后端分页） */
const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter(
    (s) =>
      (s.userName || '').toLowerCase().includes(kw) || (s.nickName || '').toLowerCase().includes(kw)
  )
})

/** 当前页数据：在线人数有限，前端切片即可，避免为零星数据再加一套后端分页 */
const paged = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

const total = computed(() => filtered.value.length)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

/** 过滤条件变化时回到第一页 */
watch(keyword, () => {
  page.value = 1
})

/** 数据变少（强制下线后刷新）时收敛页码，避免停在空页 */
watch(total, (count) => {
  const max = Math.max(1, Math.ceil(count / pageSize.value))
  if (page.value > max) page.value = max
})

/** 分页变化 */
function onPageChange(nextPage, nextSize) {
  pageSize.value = nextSize
  page.value = nextPage
}

/** 剩余有效期：秒 → 天/时/分 */
function formatRemain(seconds) {
  if (seconds === null || seconds === undefined) return '—'
  const s = Number(seconds)
  if (!Number.isFinite(s) || s <= 0) return '已过期'
  const days = Math.floor(s / 86400)
  const hours = Math.floor((s % 86400) / 3600)
  const minutes = Math.floor((s % 3600) / 60)
  if (days > 0) return `${days} 天 ${hours} 小时`
  if (hours > 0) return `${hours} 小时 ${minutes} 分`
  return `${minutes} 分`
}

/** 加载在线列表 */
async function loadData() {
  loading.value = true
  try {
    const res = await sessionApi.queryOnlineSessions()
    list.value = res.data || []
  } catch (e) {
    // 错误提示由拦截器统一处理；这里保留上一次数据，避免误判为"没人在线"
    console.error('[onlineSession] 加载在线会话失败：', e)
  } finally {
    loading.value = false
  }
}

/** 强制下线单个账号（二次确认：这是会影响他人的操作） */
async function handleKick(row) {
  try {
    await ElMessageBox.confirm(
      `确定让「${row.userName}」立即下线吗？其当前令牌会失效，下一次请求需重新登录。`,
      '强制下线',
      { type: 'warning', confirmButtonText: '确定下线', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await sessionApi.kickSession(row.userName, row.deviceId)
    ElMessage.success(`已让「${row.userName}」下线`)
    loadData()
  } catch (e) {
    console.error('[onlineSession] 强制下线失败：', e)
  }
}

/** 全部下线：跳过当前会话（否则自己也会被踢） */
async function handleKickAll() {
  const targets = list.value.filter((s) => !s.current)
  if (targets.length === 0) {
    ElMessage.info('除当前会话外，没有其他在线账号')
    return
  }
  try {
    await ElMessageBox.confirm(
      `将让除当前会话外的 ${targets.length} 个账号全部下线，确定继续吗？`,
      '全部下线',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  loading.value = true
  try {
    // 逐个调用：后端按账号吊销，逐个调用便于失败时定位到具体账号
    for (const s of targets) {
      await sessionApi.kickSession(s.userName, s.deviceId)
    }
    ElMessage.success(`已下线 ${targets.length} 个账号`)
    await loadData()
  } catch (e) {
    console.error('[onlineSession] 批量下线失败：', e)
    ElMessage.warning('部分账号下线失败，请刷新后重试')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.session-digest {
  font-family: var(--lc-font-mono);
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-muted);
}
.session-hint {
  margin: var(--lc-sp-3) 0 0;
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-faint);
}

/* ===== V2 页面细节 ===== */
/* 令牌摘要：只显示前 8 位，用等宽字体 + 胶囊底，强调"这是标识不是凭证" */
.session-digest {
  display: block;
  max-width: 100%;
  overflow: hidden;
  padding: 1px 8px;
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-pill);
  background: var(--lc-surface-soft);
  font-family: var(--lc-font-mono);
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-muted);
}
.session-hint {
  margin: var(--lc-sp-3) 0 0;
  font-size: var(--lc-fs-hint);
  line-height: 1.7;
  color: var(--lc-ink-faint);
}
</style>
