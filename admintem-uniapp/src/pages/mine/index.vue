<template>
  <view class="page page--tabbar mine">
    <!-- 资料头：与工作台同一套渐变语言 -->
    <view class="hero">
      <view class="hero__row">
        <image v-if="avatarUrl" class="hero__avatar-img" :src="avatarUrl" mode="aspectFill" />
        <view v-else class="hero__avatar">{{ avatarText }}</view>
        <view class="grow">
          <text class="hero__name">{{ user?.nickName || user?.userName || '未登录' }}</text>
          <text class="hero__account">@{{ user?.userName || '—' }}</text>
        </view>
      </view>

      <view class="hero__roles">
        <text v-for="role in roles" :key="role.id || role.roleKey" class="hero__chip">
          {{ role.roleName || role.roleKey }}
        </text>
        <text v-if="!roles.length" class="hero__chip">未分配角色</text>
      </view>
    </view>

    <!-- 概览 -->
    <view class="card mt-24">
      <text class="card__title">账号概览</text>
      <view class="grid">
        <view class="grid__item">
          <text class="grid__num">{{ menuCount }}</text>
          <text class="grid__label">可访问菜单</text>
        </view>
        <view class="grid__item">
          <text class="grid__num">{{ permCount }}</text>
          <text class="grid__label">权限标识</text>
        </view>
        <view class="grid__item">
          <text class="grid__num">{{ roleCount }}</text>
          <text class="grid__label">角色数量</text>
        </view>
      </view>
    </view>

    <!-- 自助操作 -->
    <view class="card">
      <text class="card__title">账号设置</text>
      <view class="cell" @tap="go('/pages/profile/index')">
        <text class="cell__label">个人资料</text>
        <view class="row gap-8">
          <text class="cell__value">{{ user?.nickName || '未设置' }}</text>
          <view class="chevron" />
        </view>
      </view>
      <view class="cell" @tap="go('/pages/password/index')">
        <text class="cell__label">修改密码</text>
        <view class="row gap-8">
          <text class="cell__value">建议定期更换</text>
          <view class="chevron" />
        </view>
      </view>
      <view class="cell" @tap="showAbout">
        <text class="cell__label">关于</text>
        <view class="row gap-8">
          <text class="cell__value">v1.0.0</text>
          <view class="chevron" />
        </view>
      </view>
    </view>

    <!-- 账号信息 -->
    <view class="card">
      <text class="card__title">登录信息</text>
      <view class="cell">
        <text class="cell__label">手机号</text>
        <text class="cell__value">{{ user?.phone || '未绑定' }}</text>
      </view>
      <view class="cell">
        <text class="cell__label">邮箱</text>
        <text class="cell__value">{{ user?.email || '未绑定' }}</text>
      </view>
      <view class="cell">
        <text class="cell__label">账号状态</text>
        <text class="chip" :class="user?.status === 0 ? 'chip--success' : ''">
          {{ user?.status === 0 ? '启用' : '禁用' }}
        </text>
      </view>
      <view class="cell">
        <text class="cell__label">用户 ID</text>
        <text class="cell__value">{{ user?.userId || '—' }}</text>
      </view>
    </view>

    <button class="btn btn--danger-ghost btn--block mt-24" @tap="handleLogout">退出登录</button>

    <view class="mine__foot">
      <text class="faint">admintem 权限管理系统 · uni-app 客户端</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 我的（tabBar 第二页）
 *
 * 展示身份与账号信息，提供三个自助动作：改资料、改密码、退出。
 * 退出会先调 /auth/logout 让服务端白名单失效，再清本地并回登录页。
 */
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { user, avatarText, refreshUser, logout, flattenMenus } from '@/stores/user'
import { getToken } from '@/utils/storage'
import { resolveAssetUrl } from '@/utils/media'

const roles = computed(() => (user.value && user.value.roles) || [])
const menuCount = computed(() => flattenMenus().length)
const permCount = computed(() => (((user.value && user.value.permissions) || []).length))
const roleCount = computed(() => roles.value.length)
const avatarUrl = computed(() => resolveAssetUrl(user.value && user.value.avatar))

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  refreshUser().catch(() => {})
})

function go(url) {
  uni.navigateTo({ url })
}

function showAbout() {
  uni.showModal({
    title: '关于',
    content:
      'admintem 权限管理系统 · uni-app 客户端 v1.0.0\n后端：admintem-backend（与桌面端、移动端共用）\n鉴权：admintem_token: LAUNCH <jwt>',
    showCancel: false,
    confirmText: '知道了',
  })
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
.mine {
  padding-top: 20rpx;
}

.hero {
  padding: 36rpx 32rpx 28rpx;
  border-radius: var(--r-card);
  background-color: var(--c-brand);
  background-image: var(--grad-hero);
  box-shadow: 0 24rpx 56rpx -36rpx rgba(30, 58, 110, 0.65);
}

.hero__row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 24rpx;
}

.hero__avatar,
.hero__avatar-img {
  width: 112rpx;
  height: 112rpx;
  flex: 0 0 112rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  border: 1rpx solid rgba(255, 255, 255, 0.34);
  color: #ffffff;
  font-size: 44rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero__name {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #ffffff;
}

.hero__account {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.76);
}

.hero__roles {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.18);
}

.hero__chip {
  padding: 4rpx 18rpx;
  border-radius: var(--r-pill);
  background: rgba(255, 255, 255, 0.16);
  border: 1rpx solid rgba(255, 255, 255, 0.28);
  font-size: 22rpx;
  color: #ffffff;
}

.grid {
  display: flex;
  flex-direction: row;
  margin-top: 16rpx;
}

.grid__item {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.grid__item + .grid__item {
  padding-left: 24rpx;
  border-left: 1rpx solid var(--c-line-soft);
}

.grid__num {
  font-size: 40rpx;
  font-weight: 700;
  color: var(--c-brand);
}

.grid__label {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--c-muted);
}

.mine__foot {
  margin-top: 40rpx;
  text-align: center;
}
</style>
