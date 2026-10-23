<template>
  <view class="auth">
    <!-- 画布：渐变 + 网格 + 两团柔光（纯装饰，不参与交互） -->
    <view class="auth__bg" />
    <view class="auth__glow auth__glow--a" />
    <view class="auth__glow auth__glow--b" />

    <view class="auth__inner" :style="{ paddingTop: statusH + 24 + 'px' }">
      <view class="brand">
        <image class="brand__mark" src="/static/logo.png" mode="aspectFit" />
        <view class="brand__text">
          <text class="brand__name">admintem</text>
          <text class="brand__sub">权限管理系统</text>
          </view>
      </view>

      <view class="auth__head">
        <text class="kicker">SIGN IN</text>
        <text class="auth__title">登录</text>
        <text class="auth__desc">使用你的账号继续访问工作台</text>
          </view>

      <view class="card auth__card">
        <view class="field">
          <text class="field__label">账号</text>
          <input
            v-model="form.username"
            class="input"
            type="text"
            :maxlength="32"
            placeholder="请输入账号"
            placeholder-class="input-placeholder"
            confirm-type="next"
          />
        </view>

        <view class="field">
          <text class="field__label">密码</text>
          <view class="field__control">
            <input
              v-model="form.password"
              class="input"
              :password="!showPassword"
              :maxlength="32"
              placeholder="请输入密码"
              placeholder-class="input-placeholder"
              confirm-type="done"
              @confirm="handleSubmit"
            />
            <text class="field__eye" @tap="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</text>
          </view>
        </view>

        <view class="row between auth__meta">
          <view class="row gap-8" @tap="form.rememberMe = !form.rememberMe">
            <view class="check" :class="{ 'check--on': form.rememberMe }">
              <text v-if="form.rememberMe" class="check__tick">✓</text>
          </view>
            <text class="faint">记住账号</text>
          </view>
          <text class="faint">令牌 7 天有效</text>
          </view>

        <button
          class="btn btn--primary btn--block"
          :class="{ 'btn--disabled': loading }"
          @tap="handleSubmit"
        >
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </view>

      <view class="row auth__foot">
        <text class="muted">还没有账号？</text>
        <text class="auth__link" @tap="goRegister">创建账号</text>
          </view>

      <view class="notice notice--muted auth__demo">
        <text>演示账号：admin / 12345678（普通账号 test / 12345678）</text>
          </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 登录页
 *
 * 与桌面端/移动端共用 POST /auth/login；成功后令牌写入本地并 reLaunch 到工作台。
 * 已登录用户进入本页会直接跳工作台（避免"返回键回到登录页"的常见问题）。
 */
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { login } from '@/stores/user'
import { getToken, getRememberedName, setRememberedName } from '@/utils/storage'

const statusH = uni.getSystemInfoSync().statusBarHeight || 20
const loading = ref(false)
const showPassword = ref(false)
const form = reactive({
  username: getRememberedName() || '',
  password: '',
  rememberMe: !!getRememberedName(),
})

onShow(() => {
  if (getToken()) {
    uni.reLaunch({ url: '/pages/home/index' })
  }
})

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}

function goRegister() {
  uni.navigateTo({ url: '/pages/register/index' })
}

async function handleSubmit() {
  if (loading.value) return
  if (!form.username.trim()) {
    toast('请输入账号')
    return
  }
  if (!form.password) {
    toast('请输入密码')
    return
  }
  loading.value = true
  try {
    await login({
      username: form.username.trim(),
      password: form.password,
      rememberMe: form.rememberMe,
    })
    setRememberedName(form.rememberMe ? form.username.trim() : '')
    uni.showToast({ title: '登录成功', icon: 'success', duration: 900 })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/home/index' })
    }, 600)
  } catch (e) {
    // 请求层已经弹过错误提示（登录接口不静默），这里只恢复按钮状态
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background-color: var(--c-bg);
  background-image: linear-gradient(180deg, #f8fafc 0%, #eef3fb 52%, #e7eef9 100%);
}

/* 网格：两层 linear-gradient + background-size，小程序同样支持 */
.auth__bg {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background-image: linear-gradient(rgba(15, 23, 42, 0.035) 1rpx, transparent 1rpx),
    linear-gradient(90deg, rgba(15, 23, 42, 0.035) 1rpx, transparent 1rpx);
  background-size: 56rpx 56rpx;
}

.auth__glow {
  position: absolute;
  border-radius: 50%;
  opacity: 0.55;
}

.auth__glow--a {
  width: 520rpx;
  height: 520rpx;
  right: -180rpx;
  top: -140rpx;
  background-image: radial-gradient(circle, rgba(47, 111, 237, 0.16) 0%, rgba(47, 111, 237, 0) 68%);
}

.auth__glow--b {
  width: 620rpx;
  height: 620rpx;
  left: -260rpx;
  bottom: -320rpx;
  background-image: radial-gradient(circle, rgba(30, 58, 110, 0.18) 0%, rgba(30, 58, 110, 0) 66%);
}

.auth__inner {
  position: relative;
  z-index: 1;
  padding-left: 40rpx;
  padding-right: 40rpx;
  padding-bottom: calc(56rpx + env(safe-area-inset-bottom));
}

.brand {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 20rpx;
}

.brand__mark {
  width: 76rpx;
  height: 76rpx;
  border-radius: 22rpx;
  box-shadow: 0 12rpx 28rpx -18rpx rgba(30, 58, 110, 0.6);
}

.brand__text {
  display: flex;
  flex-direction: column;
}

.brand__name {
  font-size: 32rpx;
  font-weight: 700;
  letter-spacing: 0.5rpx;
  color: var(--c-ink);
}

.brand__sub {
  font-size: 22rpx;
  letter-spacing: 2rpx;
  color: var(--c-faint);
}

.auth__head {
  display: flex;
  flex-direction: column;
  margin-top: 72rpx;
  margin-bottom: 40rpx;
}

.auth__title {
  margin-top: 12rpx;
  font-size: 64rpx;
  font-weight: 700;
  letter-spacing: -1rpx;
  color: var(--c-ink);
}

.auth__desc {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: var(--c-muted);
}

.auth__card {
  padding: 36rpx 32rpx 32rpx;
}

.auth__meta {
  margin: 4rpx 0 32rpx;
}

/* 自绘复选框：小程序原 checkbox 样式难统一，这里用 view + 对勾 */
.check {
  width: 34rpx;
  height: 34rpx;
  border-radius: 8rpx;
  border: 2rpx solid var(--c-line-input);
  background: var(--c-card);
  display: flex;
  align-items: center;
  justify-content: center;
}

.check--on {
  background: var(--c-brand);
  border-color: var(--c-brand);
}

.check__tick {
  font-size: 22rpx;
  line-height: 1;
  color: #ffffff;
}

.auth__foot {
  justify-content: center;
  margin-top: 36rpx;
  gap: 8rpx;
}

.auth__link {
  font-size: 26rpx;
  font-weight: 600;
  color: var(--c-brand);
}

/* 密码可见切换：不改变输入框高度，只占右侧一小段 */
.field__control {
  position: relative;
  flex: 1;
  min-width: 0;
}
.field__eye {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  padding: 8rpx 4rpx;
  font-size: 24rpx;
  color: var(--c-primary);
}
.field__control .input {
  padding-right: 72rpx;
}

.auth__demo {
  margin-top: 28rpx;
}
</style>
