<template>
  <view class="auth">
    <view class="auth__bg" />
    <view class="auth__glow auth__glow--a" />

    <view class="auth__inner" :style="{ paddingTop: statusH + 24 + 'px' }">
      <view class="row between auth__nav">
        <view class="row gap-16" @tap="goBack">
          <view class="back" />
          <text class="auth__nav-text">返回登录</text>
          </view>
        <text class="kicker">SIGN UP</text>
          </view>

      <view class="auth__head">
        <text class="auth__title">创建账号</text>
        <text class="auth__desc">填写基本信息，注册后即可登录系统</text>
          </view>

      <view class="card auth__card">
        <view class="field">
          <text class="field__label">账号</text>
          <input
            v-model="form.userName"
            class="input"
            :maxlength="32"
            placeholder="4~32 位字母、数字或下划线"
            placeholder-class="input-placeholder"
          />
        </view>

        <view class="field">
          <text class="field__label">昵称</text>
          <input
            v-model="form.nickName"
            class="input"
            :maxlength="32"
            placeholder="展示在系统里的名字"
            placeholder-class="input-placeholder"
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
              placeholder="6~32 位"
              placeholder-class="input-placeholder"
            />
            <text class="field__eye" @tap="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</text>
          </view>
        </view>

        <view class="field">
          <text class="field__label">确认密码</text>
          <view class="field__control">
            <input
              v-model="form.confirmPassword"
              class="input"
              :password="!showConfirm"
              :maxlength="32"
              placeholder="再次输入密码"
              placeholder-class="input-placeholder"
            />
            <text class="field__eye" @tap="showConfirm = !showConfirm">{{ showConfirm ? '隐藏' : '显示' }}</text>
          </view>
        </view>

        <view class="field">
          <text class="field__label">邮箱（选填）</text>
          <input
            v-model="form.email"
            class="input"
            type="text"
            :maxlength="64"
            placeholder="name@example.com"
            placeholder-class="input-placeholder"
          />
        </view>

        <view class="field">
          <text class="field__label">手机号（选填）</text>
          <input
            v-model="form.phone"
            class="input"
            type="number"
            :maxlength="11"
            placeholder="11 位手机号"
            placeholder-class="input-placeholder"
          />
        </view>

        <view class="field">
          <text class="field__label">性别</text>
          <view class="sex" role="radiogroup" aria-label="性别">
            <view
              v-for="option in sexOptions"
              :key="option.value"
              class="sex__item"
              :class="{ 'sex__item--on': form.sex === option.value }"
              @tap="form.sex = option.value"
            >
              <text>{{ option.label }}</text>
          </view>
          </view>
        </view>

        <button
          class="btn btn--primary btn--block mt-8"
          :class="{ 'btn--disabled': loading }"
          @tap="handleSubmit"
        >
          {{ loading ? '提交中…' : '创建账号' }}
        </button>
      </view>

      <view class="notice notice--muted mt-24">
        <text>注册是否开放由后端配置 admintem.register-enabled 控制；账号创建后默认无角色权限。</text>
          </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 注册页
 *
 * 校验规则与后端 RegisterRequest 对齐（前端先拦一遍，减少一次往返）：
 *   账号 4~32 位字母/数字/下划线；密码 6~32；手机号 11 位或留空。
 */
import { reactive, ref } from 'vue'
import { register } from '@/api/auth'

const statusH = uni.getSystemInfoSync().statusBarHeight || 20
const loading = ref(false)
const showPassword = ref(false)
const showConfirm = ref(false)
const sexOptions = [
  { value: 1, label: '男' },
  { value: 0, label: '女' },
]

const form = reactive({
  userName: '',
  nickName: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  sex: 1,
})

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}

function goBack() {
  uni.reLaunch({ url: '/pages/login/index' })
}

function validate() {
  if (!/^[a-zA-Z0-9_]{4,32}$/.test(form.userName.trim())) {
    toast('账号须为 4~32 位字母、数字或下划线')
    return false
  }
  if (!form.nickName.trim()) {
    toast('请输入昵称')
    return false
  }
  if (form.password.length < 6 || form.password.length > 32) {
    toast('密码长度须为 6~32 位')
    return false
  }
  if (form.password !== form.confirmPassword) {
    toast('两次输入的密码不一致')
    return false
  }
  if (form.phone && !/^1\d{10}$/.test(form.phone)) {
    toast('手机号格式不正确')
    return false
  }
  return true
}

async function handleSubmit() {
  if (loading.value || !validate()) return
  loading.value = true
  try {
    await register({
      userName: form.userName.trim(),
      nickName: form.nickName.trim(),
      password: form.password,
      email: form.email.trim() || undefined,
      phone: form.phone.trim() || undefined,
      sex: form.sex,
    })
    uni.showToast({ title: '注册成功，请登录', icon: 'none', duration: 1400 })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/login/index' })
    }, 1200)
  } catch (e) {
    // 请求层已提示具体原因
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
  opacity: 0.5;
}

.auth__glow--a {
  width: 560rpx;
  height: 560rpx;
  right: -220rpx;
  top: -180rpx;
  background-image: radial-gradient(circle, rgba(47, 111, 237, 0.16) 0%, rgba(47, 111, 237, 0) 68%);
}

.auth__inner {
  position: relative;
  z-index: 1;
  padding-left: 40rpx;
  padding-right: 40rpx;
  padding-bottom: calc(56rpx + env(safe-area-inset-bottom));
}

.auth__nav {
  height: 72rpx;
}

.auth__nav-text {
  font-size: 26rpx;
  color: var(--c-muted);
}

.back {
  width: 18rpx;
  height: 18rpx;
  border-left: 3rpx solid var(--c-ink);
  border-bottom: 3rpx solid var(--c-ink);
  transform: rotate(45deg);
}

.auth__head {
  display: flex;
  flex-direction: column;
  margin-top: 24rpx;
  margin-bottom: 32rpx;
}

.auth__title {
  font-size: 60rpx;
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
.field__control {
  position: relative;
  flex: 1;
  min-width: 0;
}
.field__control .input {
  width: 100%;
  box-sizing: border-box;
  padding-right: 76rpx;
}
.field__eye {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  z-index: 1;
  padding: 8rpx 4rpx;
  font-size: 24rpx;
  color: var(--c-primary, #1e3a6e);
}
.sex {
  display: inline-flex;
  gap: 4rpx;
  padding: 6rpx;
  background: var(--c-surface-soft, #f1f5fb);
  border: 1rpx solid var(--c-line, #e2e8f0);
  border-radius: 18rpx;
}
.sex__item {
  min-width: 108rpx;
  padding: 10rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: var(--c-muted, #64748b);
  border-radius: 12rpx;
  transition: color 0.15s ease, background 0.15s ease, box-shadow 0.15s ease;
}
.sex__item--on {
  color: var(--c-primary, #1e3a6e);
  font-weight: 600;
  background: #fff;
  box-shadow: 0 2rpx 6rpx rgba(15, 23, 42, 0.08), 0 0 0 1rpx var(--c-line, #e2e8f0);
}
</style>
