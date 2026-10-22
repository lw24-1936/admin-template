<template>
  <view class="page page--tabbar home">
    <!-- 英雄卡：问候 + 身份 + 三项指标 -->
    <view class="hero">
      <view class="hero__top">
        <view class="hero__avatar">{{ avatarText }}</view>
        <view class="grow">
          <text class="hero__hi">{{ greeting }}，{{ user?.nickName || user?.userName || '访客' }}</text>
          <text class="hero__role">{{ roleText }}</text>
        </view>
        <view class="hero__badge">
          <text class="hero__badge-text">{{ user?.status === 0 ? '已启用' : '已禁用' }}</text>
        </view>
      </view>

      <view class="hero__stats">
        <view class="stat">
          <text class="stat__num">{{ menuCount }}</text>
          <text class="stat__label">可访问菜单</text>
        </view>
        <view class="stat">
          <text class="stat__num">{{ permCount }}</text>
          <text class="stat__label">权限标识</text>
        </view>
        <view class="stat">
          <text class="stat__num">{{ roleCount }}</text>
          <text class="stat__label">角色数量</text>
        </view>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="card mt-24">
      <text class="card__title">快捷入口</text>
      <text class="card__desc">个人资料与密码修改在小程序内完成；列表维护类页面仍在桌面端。</text>
      <view class="quick">
        <view class="quick__item" @tap="go('/pages/profile/index')">
          <text class="quick__icon">A</text>
          <text class="quick__name">个人资料</text>
        </view>
        <view class="quick__item" @tap="go('/pages/password/index')">
          <text class="quick__icon">#</text>
          <text class="quick__name">修改密码</text>
        </view>
        <view class="quick__item" @tap="refresh">
          <text class="quick__icon">↻</text>
          <text class="quick__name">刷新权限</text>
        </view>
        <view class="quick__item" @tap="handleLogout">
          <text class="quick__icon">←</text>
          <text class="quick__name">退出登录</text>
        </view>
      </view>
    </view>

    <!-- 我的菜单 -->
    <view class="card">
      <view class="row between">
        <text class="card__title">我的菜单</text>
        <text class="faint">{{ menus.length }} 项</text>
      </view>
      <text class="card__desc">由后端按角色下发，未授权的菜单不会出现在这里。</text>

      <view v-if="menus.length" class="menu-list">
        <view v-for="menu in menus" :key="menu.id" class="menu-item">
          <view class="menu-item__left">
            <view class="dot" />
            <text class="menu-item__name">{{ menu.name }}</text>
          </view>
          <view class="row gap-8">
            <text class="menu-item__url">{{ menu.url || '—' }}</text>
            <text class="chip chip--soft">桌面端</text>
          </view>
        </view>
      </view>
      <view v-else class="empty">当前账号没有可访问的菜单</view>
    </view>

    <view class="home__foot">
      <text class="faint">admintem · uni-app 客户端 v1.0.0</text>
      <text class="faint">后端共享 admintem-backend</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 工作台（tabBar 首页）
 *
 * 数据全部来自 /auth/getUserInfo：角色、菜单、权限标识。
 * 小程序端定位是"随身查看 + 自助维护"，列表维护类页面仍由桌面端承载，
 * 因此菜单只做展示并标注「桌面端」，不做跳转（避免跳到空页面）。
 */
import { computed, ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { user, roleText, avatarText, refreshUser, logout, flattenMenus } from '@/stores/user'
import { getToken } from '@/utils/storage'

const refreshing = ref(false)

// 与下方菜单列表保持同一口径：统计叶子菜单（能真正进入的页面），而不是顶级分组
const menus = computed(() => flattenMenus())
const menuCount = computed(() => menus.value.length)
const permCount = computed(() => (((user.value && user.value.permissions) || []).length))
const roleCount = computed(() => ((user.value && user.value.roles) || []).length)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  refresh()
})

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})

/** 首屏不弹 loading：先用缓存渲染，再静默刷新 */
async function refresh() {
  if (refreshing.value) return
  refreshing.value = true
  try {
    await refreshUser()
  } catch (e) {
    // 401 已由请求层统一跳登录；其他错误保留缓存，页面不白屏
  } finally {
    refreshing.value = false
  }
}

function go(url) {
  uni.navigateTo({ url })
}

function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '退出后需要重新输入账号密码，确定继续吗？',
    confirmText: '退出',
    confirmColor: '#b02a2a',
    success: async (res) => {
      if (!res.confirm) return
      await logout()
      uni.reLaunch({ url: '/pages/login/index' })
    },
  })
}
</script>

<style scoped>
.home {
  padding-top: 20rpx;
}

/* ==================== 英雄卡 ==================== */
.hero {
  padding: 36rpx 32rpx 28rpx;
  border-radius: var(--r-card);
  background-color: var(--c-brand);
  background-image: var(--grad-hero);
  box-shadow: 0 24rpx 56rpx -36rpx rgba(30, 58, 110, 0.65);
}

.hero__top {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 20rpx;
}

.hero__avatar {
  width: 88rpx;
  height: 88rpx;
  flex: 0 0 88rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.16);
  border: 1rpx solid rgba(255, 255, 255, 0.34);
  color: #ffffff;
  font-size: 36rpx;
  font-weight: 700;
}

.hero__hi {
  display: block;
  font-size: 34rpx;
  font-weight: 650;
  color: #ffffff;
}

.hero__role {
  display: block;
  margin-top: 4rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.78);
}

.hero__badge {
  padding: 4rpx 16rpx;
  border-radius: var(--r-pill);
  background: rgba(255, 255, 255, 0.16);
  border: 1rpx solid rgba(255, 255, 255, 0.3);
}

.hero__badge-text {
  font-size: 22rpx;
  color: #ffffff;
}

.hero__stats {
  display: flex;
  flex-direction: row;
  margin-top: 32rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.18);
}

.stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.stat + .stat {
  padding-left: 24rpx;
  border-left: 1rpx solid rgba(255, 255, 255, 0.18);
}

.stat__num {
  font-size: 44rpx;
  font-weight: 700;
  line-height: 1.1;
  color: #ffffff;
}

.stat__label {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.72);
}

/* ==================== 快捷入口 ==================== */
.quick {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 16rpx;
}

.quick__item {
  width: calc((100% - 16rpx) / 2);
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16rpx;
  padding: 22rpx 24rpx;
  border: 1rpx solid var(--c-line);
  border-radius: var(--r-ctl);
  background: var(--c-soft);
}

.quick__item:active {
  background: var(--c-brand-soft);
  border-color: var(--c-brand);
}

.quick__icon {
  width: 52rpx;
  height: 52rpx;
  flex: 0 0 52rpx;
  border-radius: 14rpx;
  background: var(--c-brand-soft);
  color: var(--c-brand);
  font-size: 26rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick__name {
  font-size: 26rpx;
  color: var(--c-body);
}

/* ==================== 菜单列表 ==================== */
.menu-list {
  display: flex;
  flex-direction: column;
}

.menu-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid var(--c-line-soft);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item__left {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 14rpx;
  min-width: 0;
}

.dot {
  width: 10rpx;
  height: 10rpx;
  flex: 0 0 10rpx;
  border-radius: 50%;
  background: var(--c-brand);
}

.menu-item__name {
  font-size: 28rpx;
  color: var(--c-ink);
}

.menu-item__url {
  font-size: 22rpx;
  color: var(--c-faint);
  max-width: 240rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.home__foot {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  margin-top: 40rpx;
}
</style>
