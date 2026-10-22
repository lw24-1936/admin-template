<template>
  <view class="page">
    <view class="card">
      <text class="card__title">修改密码</text>
      <text class="card__desc">
        为了账号安全，修改成功后服务端会吊销当前令牌，需要重新登录。
      </text>

      <view class="field">
        <text class="field__label">当前密码</text>
        <input
          v-model="form.oldPassword"
          class="input"
          password
          :maxlength="32"
          placeholder="请输入当前密码"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="field">
        <text class="field__label">新密码</text>
        <input
          v-model="form.newPassword"
          class="input"
          password
          :maxlength="32"
          placeholder="6~32 位"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="field">
        <text class="field__label">确认新密码</text>
        <input
          v-model="form.confirmPassword"
          class="input"
          password
          :maxlength="32"
          placeholder="再次输入新密码"
          placeholder-class="input-placeholder"
        />
      </view>

      <button class="btn btn--primary btn--block" :class="{ 'btn--disabled': saving }" @tap="handleSubmit">
        {{ saving ? '提交中…' : '确认修改' }}
      </button>
    </view>

    <view class="notice mt-24">
      <text>密码建议使用字母 + 数字组合，不要与其他系统重复。</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 修改密码
 *
 * 失败场景由后端判定（原密码错误、新密码与旧密码相同等），
 * 成功后本端主动清空登录态并回登录页 —— 令牌已被服务端吊销，继续停留只会 401。
 */
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { updatePassword } from '@/api/auth'
import { clearLocal } from '@/stores/user'
import { getToken } from '@/utils/storage'

const saving = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}

onLoad(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
  }
})

async function handleSubmit() {
  if (saving.value) return
  if (!form.oldPassword) {
    toast('请输入当前密码')
    return
  }
  if (form.newPassword.length < 6 || form.newPassword.length > 32) {
    toast('新密码长度须为 6~32 位')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    toast('两次输入的新密码不一致')
    return
  }
  if (form.newPassword === form.oldPassword) {
    toast('新密码不能与当前密码相同')
    return
  }
  saving.value = true
  try {
    await updatePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    clearLocal()
    uni.showToast({ title: '修改成功，请重新登录', icon: 'none', duration: 1400 })
    setTimeout(() => uni.reLaunch({ url: '/pages/login/index' }), 1200)
  } catch (e) {
    // 请求层已提示
  } finally {
    saving.value = false
  }
}
</script>
