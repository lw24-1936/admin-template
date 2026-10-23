<template>
  <!-- 移动端注册：与桌面端/uni-app 共用 POST /auth/register；是否开放由后端 register-enabled 控制 -->
  <main class="m-register">
    <header class="m-register__head">
      <router-link to="/login" class="m-register__back">← 返回登录</router-link>
      <div class="m-register__brand">
        <img class="m-register__mark" src="/favicon.svg" alt="" width="28" height="28" />
        <span class="m-register__brand-text">
          <strong>admintem</strong>
          <em>权限管理系统</em>
        </span>
      </div>
      <h1 class="m-register__title">创建账号</h1>
      <p class="m-register__desc">填写基本信息，注册后即可登录；新账号默认无角色权限。</p>
    </header>

    <section class="m-register__sheet">
      <van-form @submit="handleSubmit">
        <van-cell-group inset>
          <van-field
            v-model="form.userName"
            name="userName"
            label="账号"
            placeholder="4~32 位字母、数字或下划线"
            clearable
            :rules="[
              { required: true, message: '请输入账号' },
              { pattern: /^[a-zA-Z0-9_]{4,32}$/, message: '账号须为 4~32 位字母、数字或下划线' },
            ]"
          />
          <van-field
            v-model="form.nickName"
            name="nickName"
            label="昵称"
            placeholder="展示在系统里的名字"
            clearable
            :rules="[{ required: true, message: '请输入昵称' }]"
          />
          <van-field
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            name="password"
            label="密码"
            placeholder="6~32 位"
            :right-icon="showPassword ? 'eye-o' : 'closed-eye'"
            :rules="[
              { required: true, message: '请输入密码' },
              { validator: (v) => v.length >= 6 && v.length <= 32, message: '密码长度须为 6~32 位' },
            ]"
            @click-right-icon="showPassword = !showPassword"
          />
          <van-field
            v-model="form.confirmPassword"
            :type="showConfirm ? 'text' : 'password'"
            name="confirmPassword"
            label="确认密码"
            placeholder="再次输入密码"
            :right-icon="showConfirm ? 'eye-o' : 'closed-eye'"
            :rules="[
              { required: true, message: '请再次输入密码' },
              { validator: (v) => v === form.password, message: '两次输入的密码不一致' },
            ]"
            @click-right-icon="showConfirm = !showConfirm"
          />
          <van-field
            v-model="form.email"
            name="email"
            label="邮箱"
            placeholder="选填，name@example.com"
            :rules="[{ validator: (v) => !v || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v), message: '邮箱格式不正确' }]"
          />
          <van-field
            v-model="form.phone"
            name="phone"
            label="手机号"
            type="tel"
            maxlength="11"
            placeholder="选填，11 位手机号"
            :rules="[{ validator: (v) => !v || /^1\d{10}$/.test(v), message: '手机号格式不正确' }]"
          />
        </van-cell-group>

        <div class="m-register__sex" role="radiogroup" aria-label="性别">
          <span class="m-register__sex-label">性别</span>
          <div class="m-register__seg">
            <button
              v-for="option in sexOptions"
              :key="option.value"
              type="button"
              class="m-register__seg-item"
              :class="{ 'is-active': form.sex === option.value }"
              :aria-pressed="form.sex === option.value"
              @click="form.sex = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <van-button
          block
          type="primary"
          native-type="submit"
          :loading="loading"
          loading-text="提交中..."
          class="m-register__submit"
        >
          创建账号
        </van-button>
      </van-form>

      <p class="m-register__switch">
        已有账号？
        <router-link to="/login" class="m-register__link">返回登录</router-link>
      </p>
    </section>

    <p class="m-register__note">注册开放状态由后端 admintem.register-enabled 控制。</p>
  </main>
</template>

<script setup>
/**
 * 移动端注册页
 *
 * 校验规则与后端 RegisterRequest 对齐（先在前端拦一遍，减少往返）：
 * 账号 4~32 位字母/数字/下划线；密码 6~32；手机号 11 位或留空。
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Button as VanButton,
  CellGroup as VanCellGroup,
  Field as VanField,
  Form as VanForm,
  showSuccessToast,
} from 'vant'
import 'vant/es/button/style/index'
import 'vant/es/cell-group/style/index'
import 'vant/es/field/style/index'
import 'vant/es/form/style/index'
import * as authApi from '@/api/auth'

const router = useRouter()
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

async function handleSubmit() {
  loading.value = true
  try {
    await authApi.register({
      userName: form.userName.trim(),
      nickName: form.nickName.trim(),
      password: form.password,
      email: form.email.trim() || undefined,
      phone: form.phone.trim() || undefined,
      sex: form.sex,
    })
    showSuccessToast('注册成功，请登录')
    setTimeout(() => router.replace('/login'), 900)
  } catch (e) {
    // 错误提示由 request 拦截器统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.m-register {
  min-height: 100vh;
  min-height: 100dvh;
  padding: 28px 16px calc(24px + env(safe-area-inset-bottom));
  background-color: var(--lc-paper);
  background-image:
    radial-gradient(circle at 88% -8%, rgba(47, 111, 237, 0.2), transparent 46%),
    radial-gradient(circle at -10% 32%, rgba(124, 58, 237, 0.12), transparent 42%),
    linear-gradient(var(--lc-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--lc-grid-line) 1px, transparent 1px);
  background-size: auto, auto, 28px 28px, 28px 28px;
}

.m-register__head {
  animation: m-reg-in 0.35s ease both;
}
.m-register__back {
  display: inline-block;
  margin-bottom: 20px;
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-muted);
  text-decoration: none;
}
.m-register__back:active {
  color: var(--lc-brand);
}
.m-register__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}
.m-register__mark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
}
.m-register__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}
.m-register__brand-text strong {
  font-size: var(--lc-fs-lead);
  font-weight: 650;
  color: var(--lc-ink);
}
.m-register__brand-text em {
  font-style: normal;
  font-size: var(--lc-fs-kicker);
  letter-spacing: 1.6px;
  color: var(--lc-ink-faint);
}
.m-register__title {
  margin: 0 0 8px;
  font-size: var(--lc-fs-display);
  font-weight: 650;
  letter-spacing: 0.5px;
  color: var(--lc-ink);
}
.m-register__desc {
  margin: 0 0 22px;
  font-size: var(--lc-fs-label);
  line-height: 1.8;
  color: var(--lc-ink-muted);
}

.m-register__sheet {
  padding: 18px 0 20px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-card);
  box-shadow: var(--lc-shadow-sheet);
  backdrop-filter: blur(16px) saturate(1.05);
  animation: m-reg-in 0.4s ease both;
}
.m-register__sheet :deep(.van-cell-group--inset) {
  margin: 0 16px;
}
.m-register__sheet :deep(.van-field) {
  padding: 11px 0;
}
.m-register__sheet :deep(.van-field__label) {
  width: 58px;
  color: var(--lc-ink-body);
}
.m-register__sheet :deep(.van-cell::after) {
  border-color: var(--lc-line-soft);
}

/* 性别：紧凑分段控件，和桌面端注册页同一形态 */
.m-register__sex {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 16px 4px;
}
.m-register__sex-label {
  width: 58px;
  flex: 0 0 58px;
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-body);
}
.m-register__seg {
  display: inline-flex;
  gap: 2px;
  padding: 3px;
  background: var(--lc-surface-soft);
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-ctl);
}
.m-register__seg-item {
  min-width: 64px;
  height: 32px;
  padding: 0 16px;
  font-family: inherit;
  font-size: 13px;
  color: var(--lc-ink-muted);
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: color 0.15s ease, background 0.15s ease, box-shadow 0.15s ease;
}
.m-register__seg-item.is-active {
  color: var(--lc-brand);
  font-weight: 600;
  background: var(--lc-paper-card);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08), 0 0 0 1px var(--lc-line);
}

/* Vant 的 block 会被 width:auto 覆盖，这里显式撑满（左右各留 16px） */
.m-register__submit {
  width: calc(100% - 32px);
  margin: 12px 16px 0;
  height: var(--lc-control-h);
  font-size: var(--lc-fs-lead);
  letter-spacing: 4px;
  border: none;
  border-radius: var(--lc-radius-ctl);
}

.m-register__switch {
  margin: 16px 16px 0;
  text-align: center;
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-muted);
}
.m-register__link {
  margin-left: 4px;
  color: var(--lc-brand);
  font-weight: 600;
  text-decoration: none;
}
.m-register__note {
  margin: 16px 8px 0;
  text-align: center;
  font-size: var(--lc-fs-kicker);
  color: var(--lc-ink-faint);
}

@keyframes m-reg-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
