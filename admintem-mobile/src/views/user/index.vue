<template>
  <div class="lc-page user-page">
    <van-nav-bar title="用户管理" left-text="返回" left-arrow @click-left="router.back()" />

    <van-search
      v-model="query.userName"
      placeholder="搜索账号"
      show-action
      @search="handleSearch"
      @cancel="handleSearch"
    />

    <div class="user-toolbar">
      <span class="user-toolbar__hint">共 {{ total }} 个用户</span>
      <span class="user-toolbar__hint">数据按当前角色范围过滤</span>
    </div>

    <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="loadMore">
      <article v-for="item in list" :key="item.id" class="user-card">
        <div class="user-card__head">
          <div>
            <p class="user-card__name">{{ item.nickName || item.userName }}</p>
            <p class="user-card__account">@{{ item.userName }}</p>
          </div>
          <van-tag :type="item.status === 1 ? 'danger' : 'success'" plain>
            {{ item.status === 1 ? '已禁用' : '已启用' }}
          </van-tag>
        </div>
        <dl class="user-card__meta">
          <div><dt>部门</dt><dd>{{ item.deptName || '未分配' }}</dd></div>
          <div><dt>手机</dt><dd>{{ item.phone || '—' }}</dd></div>
          <div><dt>角色</dt><dd>{{ (item.roleNames || []).join('、') || '未分配' }}</dd></div>
        </dl>
        <div v-if="canEdit" class="user-card__actions">
          <van-button
            size="small"
            :type="item.status === 1 ? 'success' : 'warning'"
            plain
            @click="toggleStatus(item)"
          >
            {{ item.status === 1 ? '启用' : '禁用' }}
          </van-button>
        </div>
      </article>
      <van-empty v-if="!loading && !list.length" description="没有用户" />
    </van-list>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NavBar as VanNavBar,
  Search as VanSearch,
  List as VanList,
  Empty as VanEmpty,
  Tag as VanTag,
  Button as VanButton,
  showConfirmDialog,
  showSuccessToast,
  showFailToast,
} from 'vant'
import 'vant/es/nav-bar/style/index'
import 'vant/es/search/style/index'
import 'vant/es/list/style/index'
import 'vant/es/empty/style/index'
import 'vant/es/tag/style/index'
import 'vant/es/button/style/index'
import { pageUsers, batchUserStatus } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('sysAuthUser:editSysAuthUser'))

const query = reactive({ pageNum: 1, pageSize: 10, userName: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const finished = ref(false)

async function fetchPage(reset = false) {
  if (loading.value) return
  loading.value = true
  try {
    const res = await pageUsers({ ...query })
    const records = res.data.records || []
    list.value = reset ? records : list.value.concat(records)
    total.value = res.data.total || 0
    finished.value = list.value.length >= total.value || records.length === 0
  } catch (e) {
    console.error('[mobileUser] 加载用户失败：', e)
    finished.value = true
  } finally {
    loading.value = false
  }
}

function loadMore() {
  if (finished.value) return
  query.pageNum += 1
  fetchPage()
}

function handleSearch() {
  query.pageNum = 1
  list.value = []
  finished.value = false
  fetchPage(true)
}

async function toggleStatus(item) {
  const next = item.status === 1 ? 0 : 1
  const action = next === 1 ? '禁用' : '启用'
  try {
    await showConfirmDialog({
      title: `确认${action}`,
      message: `确定${action}用户「${item.nickName || item.userName}」吗？`,
    })
    await batchUserStatus([item.id], next)
    item.status = next
    showSuccessToast(`已${action}`)
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      console.error('[mobileUser] 更新状态失败：', e)
      showFailToast('操作失败')
    }
  }
}

handleSearch()
</script>

<style scoped>
.user-page {
  min-height: 100vh;
  padding-bottom: 24px;
}

.user-toolbar {
  display: flex;
  justify-content: space-between;
  padding: 0 16px 8px;
  color: var(--lc-text-muted, #8a8f99);
  font-size: 12px;
}

.user-card {
  margin: 0 12px 10px;
  padding: 14px;
  background: var(--lc-surface, #fff);
  border: 1px solid var(--lc-border, #eef0f4);
  border-radius: 14px;
}

.user-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.user-card__name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--lc-text, #1f2430);
}

.user-card__account {
  margin: 4px 0 0;
  color: var(--lc-text-muted, #8a8f99);
  font-size: 12px;
}

.user-card__meta {
  display: grid;
  gap: 6px;
  margin: 12px 0 0;
}

.user-card__meta div {
  display: flex;
  gap: 10px;
  font-size: 13px;
}

.user-card__meta dt {
  flex: 0 0 36px;
  color: var(--lc-text-muted, #8a8f99);
}

.user-card__meta dd {
  margin: 0;
  color: var(--lc-text, #1f2430);
  overflow-wrap: anywhere;
}

.user-card__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
