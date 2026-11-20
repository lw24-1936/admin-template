<template>
  <!-- 首页（移动端）：问候英雄卡 + 快捷操作 + 权限入口 + 可访问菜单 -->
  <div class="lc-page home">
    <!-- 英雄卡：问候、身份、状态与三项指标 -->
    <section class="home-hero">
      <div class="home-hero__top">
        <div class="home-hero__avatar">
          <img v-if="avatarUrl" :src="avatarUrl" alt="" />
          <span v-else>{{ userStore.avatarText }}</span>
        </div>
        <div class="home-hero__info">
          <p class="home-hero__hi">{{ greeting }}，{{ userStore.displayName }}</p>
          <p class="home-hero__sub">{{ userStore.roleText }}</p>
        </div>
        <span class="home-hero__status">{{ statusText }}</span>
      </div>

      <dl class="home-hero__stats">
        <div>
          <dt>{{ userStore.menus.length }}</dt>
          <dd>可访问菜单</dd>
        </div>
        <div>
          <dt>{{ userStore.permissions.length }}</dt>
          <dd>权限标识</dd>
        </div>
        <div>
          <dt>{{ userStore.roles.length }}</dt>
          <dd>角色数量</dd>
        </div>
      </dl>
    </section>

    <!-- 快捷操作 -->
    <section class="home-quick" aria-label="快捷操作">
      <button v-for="action in quickActions" :key="action.label" type="button" class="home-quick__item" @click="action.run">
        <van-icon :name="action.icon" size="20" />
        <span>{{ action.label }}</span>
      </button>
    </section>

    <!-- 权限入口：按 permission_key 控制是否渲染 -->
    <section class="lc-card">
      <div class="home-section__head">
        <h2 class="lc-card__title">管理入口</h2>
        <span class="home-section__hint">按角色权限渲染</span>
      </div>
      <div class="home-entries">
        <button
          v-for="entry in entries"
          :key="entry.perm"
          v-perm="entry.perm"
          class="home-entry"
          type="button"
          @click="openEntry(entry)"
        >
          <van-icon :name="entry.icon" size="22" />
          <span>{{ entry.label }}</span>
        </button>
      </div>
      <p v-if="visibleEntryCount === 0" class="home-empty">暂无可用管理入口</p>
    </section>

    <!-- 可访问菜单（后端已按权限过滤） -->
    <section class="lc-card">
      <div class="home-section__head">
        <h2 class="lc-card__title">我的菜单</h2>
        <span class="home-section__hint">{{ flatMenus.length }} 项</span>
      </div>
      <van-cell
        v-for="menu in flatMenus"
        :key="menu.id"
        :title="menu.name"
        :label="menu.menuUrl"
        is-link
        @click="notifyDesktop(menu.name)"
      />
      <p v-if="!flatMenus.length" class="home-empty">暂无可见菜单</p>
    </section>

    <p class="home-foot">admintem 移动端 v0.1.0 · 完整管理功能请使用桌面端</p>
  </div>
</template>

<script setup>
/**
 * 首页
 * - 数据全部来自 /auth/getUserInfo 返回的会话（用户、角色、菜单、权限）
 * - 管理入口使用 v-perm 指令按 permission_key 控制渲染
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Cell as VanCell, Icon as VanIcon, showToast, showSuccessToast } from 'vant'
import 'vant/es/cell/style/index'
import 'vant/es/icon/style/index'
import { useUserStore } from '@/stores/user'
import { flattenVisibleLeafMenus } from '@/utils/menu'

const router = useRouter()
const userStore = useUserStore()

const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

/** 头像：后端可能返回相对路径（本地存储）或绝对地址（MinIO） */
const avatarUrl = computed(() => {
  const path = userStore.user?.avatar
  if (!path) return ''
  if (/^(https?:)?\/\//i.test(path) || path.startsWith('data:') || path.startsWith('blob:')) {
    return path
  }
  return API_BASE + (path.startsWith('/') ? path : '/' + path)
})

/** 问候语随时段变化 */
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const statusText = computed(() => (userStore.user?.status === 1 ? '已禁用' : '已启用'))

/**
 * 管理入口：permission_key 与后端 @PreAuthorize、菜单 permission_key 完全一致
 * 菜单级标识来自 sys_auth_permission(type=0)，按钮级标识来自 type=1
 */
const entries = [
  { label: '用户管理', perm: 'userInfo:userInfo', icon: 'friends-o' },
  { label: '角色管理', perm: 'userInfo:roleInfo', icon: 'manager-o' },
  { label: '权限管理', perm: 'userInfo:permissionInfo', icon: 'setting-o' },
  { label: '新增用户', perm: 'sysAuthUser:addSysAuthUser', icon: 'plus' },
]

/** 快捷操作：移动端能闭环的自助动作 */
const quickActions = [
  { label: '个人中心', icon: 'user-circle-o', run: () => router.push('/mine') },
  { label: '刷新会话', icon: 'replay', run: refreshSession },
  { label: '账号设置', icon: 'setting-o', run: () => router.push('/mine') },
  { label: '桌面端', icon: 'desktop-o', run: () => notifyDesktop('完整管理功能') },
]

/**
 * 「我的菜单」清单：仅保留需要展示的叶子菜单（过滤 show=1 的隐藏菜单，
 * 隐藏菜单仍可 URL 直达，只是不出现在清单里）。
 */
const flatMenus = computed(() => flattenVisibleLeafMenus(userStore.menus))

/** 实际可见的管理入口数量（v-perm 是命令式的，这里用同一判定单独统计） */
const visibleEntryCount = computed(() =>
  entries.filter((entry) => userStore.hasPerm(entry.perm)).length
)

/** 重新拉取会话，刷新菜单/权限缓存 */
async function refreshSession() {
  try {
    await userStore.fetchUserInfo()
    showSuccessToast('会话已刷新')
  } catch (e) {
    // 错误提示由拦截器统一处理
  }
}

/** 移动端暂不承载完整管理功能，给出明确提示 */
function notifyDesktop(name) {
  showToast({ message: `${name} 请在桌面端使用`, position: 'bottom' })
}

/** 移动端已闭环的入口直接跳转，其余仍提示使用桌面端。 */
function openEntry(entry) {
  if (entry.label === '用户管理') {
    router.push('/users')
    return
  }
  notifyDesktop(entry.label)
}
</script>

<style scoped>
.home-hero {
  padding: 18px 16px 16px;
  margin-bottom: 14px;
  color: var(--lc-paper-card);
  background-color: var(--lc-brand);
  background-image: var(--lc-gradient-brand);
  border: 1px solid var(--lc-brand-deep);
  border-radius: var(--lc-radius-card);
  box-shadow: 0 16px 32px -22px var(--lc-brand-glow);
}
.home-hero__top {
  display: flex;
  align-items: center;
  gap: 12px;
}
.home-hero__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  overflow: hidden;
  font-size: 19px;
  font-weight: 600;
  color: var(--lc-paper-card);
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.34);
  border-radius: 50%;
}
.home-hero__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.home-hero__info {
  flex: 1;
  min-width: 0;
}
.home-hero__hi {
  margin: 0 0 3px;
  font-size: var(--lc-fs-card);
  font-weight: 650;
  color: var(--lc-paper-card);
}
.home-hero__sub {
  margin: 0;
  font-size: var(--lc-fs-hint);
  color: rgba(255, 255, 255, 0.78);
}
.home-hero__status {
  flex: 0 0 auto;
  padding: 4px 9px;
  font-size: var(--lc-fs-kicker);
  color: var(--lc-paper-card);
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 999px;
}

.home-hero__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 18px 0 0;
  padding: 12px 0 0;
  border-top: 1px solid rgba(255, 255, 255, 0.18);
}
.home-hero__stats div {
  text-align: center;
}
.home-hero__stats dt {
  font-size: 24px;
  font-weight: 680;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
  color: var(--lc-paper-card);
}
.home-hero__stats dd {
  margin: 3px 0 0;
  font-size: var(--lc-fs-hint);
  color: rgba(255, 255, 255, 0.78);
}

/* 快捷操作：横向四格，按下有反馈 */
.home-quick {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
  margin-bottom: 14px;
  padding: 12px 8px;
  background: var(--lc-paper-card);
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-card);
}
.home-quick__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 6px 2px;
  font-family: inherit;
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-muted);
  background: transparent;
  border: none;
  border-radius: var(--lc-radius-ctl);
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}
.home-quick__item :deep(.van-icon) {
  color: var(--lc-brand);
}
.home-quick__item:active {
  color: var(--lc-brand);
  background: var(--lc-brand-soft);
}

.home-section__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}
.home-section__hint {
  font-size: var(--lc-fs-kicker);
  color: var(--lc-ink-faint);
}

.home-entries {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.home-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 4px;
  font-family: inherit;
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-body);
  background: var(--lc-surface-soft);
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-ctl);
  cursor: pointer;
  transition: border-color var(--lc-motion-fast), background-color var(--lc-motion-fast),
    transform var(--lc-motion-fast) var(--lc-ease-out);
}
.home-entry:active {
  border-color: var(--lc-brand);
  background: var(--lc-brand-soft);
  transform: scale(0.98);
}
.home-entry :deep(.van-icon) {
  color: var(--lc-brand);
}

.home-empty {
  margin: 6px 0 0;
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-faint);
}

.home-foot {
  margin: 6px 0 0;
  font-size: var(--lc-fs-kicker);
  color: var(--lc-ink-faint);
  text-align: center;
}

.home :deep(.van-cell) {
  padding-left: 0;
  padding-right: 0;
}
.home :deep(.van-cell__label) {
  font-size: var(--lc-fs-kicker);
  color: var(--lc-ink-faint);
}

@media (prefers-reduced-motion: reduce) {
  .home-entry {
    transition: none;
  }
}
</style>
