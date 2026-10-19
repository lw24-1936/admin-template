<template>
  <!-- 移动端登录页：纸面极简（与桌面端同套视觉语言）
       单列窄卡、大号输入区、底部提示；无渐变与光晕 -->
  <main class="m-login">
    <header class="m-login__head">
      <!-- 品牌锁定：与桌面端侧边栏同一套标记（26px 主色方块 + 双行） -->
      <div class="m-login__brand">
        <span class="m-login__mark">A</span>
        <span class="m-login__brand-text">
          <strong>admintem</strong>
          <em>权限管理系统</em>
        </span>
      </div>
      <h1 class="m-login__title">移动端登录</h1>
      <p class="m-login__desc">
        菜单与按钮由同一套权限标识驱动，登录后可使用首页与个人中心。
      </p>
    </header>

    <section class="m-login__sheet">
      <van-form @submit="handleLogin">
        <van-cell-group inset>
          <van-field
            v-model="form.username"
            name="username"
            label="账号"
            placeholder="请输入账号"
            clearable
            autocomplete="username"
            :rules="[{ required: true, message: '请输入账号' }]"
          />
          <van-field
            v-model="form.password"
            type="password"
            name="password"
            label="密码"
            placeholder="请输入密码"
            autocomplete="current-password"
            :rules="[{ required: true, message: '请输入密码' }]"
          />
        </van-cell-group>

        <div class="m-login__meta">
          <van-checkbox v-model="form.rememberMe" shape="square" icon-size="15px">
            记住我（7 天内免登录）
          </van-checkbox>
        </div>

        <van-button
          block
          type="primary"
          native-type="submit"
          :loading="loading"
          loading-text="登录中..."
          class="m-login__submit"
        >
          登 录
        </van-button>
      </van-form>

      <p class="m-login__foot">
        演示账号：admin / 12345678
      </p>
    </section>

    <p class="m-login__version">admintem 移动端 v0.1.0</p>
  </main>
</template>

<script setup>
/**
 * 移动端登录页
 * - 复用桌面端同一登录接口 /auth/login，令牌同样存放于 admintem_token
 * - 支持记住我（延长令牌有效期）
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Button as VanButton,
  CellGroup as VanCellGroup,
  Checkbox as VanCheckbox,
  Field as VanField,
  Form as VanForm,
  showSuccessToast,
} from 'vant'
import 'vant/es/button/style/index'
import 'vant/es/cell-group/style/index'
import 'vant/es/checkbox/style/index'
import 'vant/es/field/style/index'
import 'vant/es/form/style/index'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '12345678',
  rememberMe: false,
})

/** 提交登录（表单校验由 van-form 完成） */
async function handleLogin() {
  loading.value = true
  try {
    await userStore.login({ ...form })
    showSuccessToast('登录成功')
    const redirect = route.query.redirect
    router.replace(redirect && typeof redirect === 'string' ? redirect : '/home')
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.m-login {
  min-height: 100vh;
  min-height: 100dvh;
  padding: 56px 16px 24px;
  background:
    linear-gradient(var(--lc-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--lc-grid-line) 1px, transparent 1px),
    var(--lc-paper);
  background-size: 28px 28px;
}

.m-login__head {
  margin-bottom: 26px;
  animation: m-login-in 0.35s ease both;
}
/* 品牌锁定：与桌面端侧边栏一致（主色方块 + 名称 + 副标题） */
.m-login__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 22px;
}
.m-login__mark {
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--lc-brand);
  color: var(--lc-paper-card);
  border-radius: 8px;
  font-size: var(--lc-fs-body);
  font-weight: 700;
}
.m-login__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}
.m-login__brand-text strong {
  font-size: var(--lc-fs-lead);
  font-weight: 650;
  letter-spacing: 0.3px;
  color: var(--lc-ink);
}
.m-login__brand-text em {
  font-style: normal;
  font-size: var(--lc-fs-kicker);
  letter-spacing: 1.6px;
  color: var(--lc-ink-faint);
}
.m-login__title {
  margin: 0 0 10px;
  font-size: var(--lc-fs-display);
  font-weight: 650;
  letter-spacing: 0.5px;
  color: var(--lc-ink);
}
.m-login__desc {
  margin: 0;
  font-size: var(--lc-fs-label);
  line-height: 1.8;
  color: var(--lc-ink-muted);
}

.m-login__sheet {
  background: var(--lc-paper-card);
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-card);
  padding: 18px 0 20px;
  /* --lc-shadow-sheet 已是完整阴影值，不能再叠加长度/颜色（曾写成 "0 20px 40px -34px var(...)" 导致整条失效） */
  box-shadow: var(--lc-shadow-sheet);
  animation: m-login-in 0.4s ease both;
}
.m-login__sheet :deep(.van-cell-group--inset) {
  margin: 0 16px;
}
.m-login__sheet :deep(.van-field) {
  padding: 12px 0;
}
.m-login__sheet :deep(.van-field__label) {
  width: 46px;
  color: var(--lc-ink-body);
}
.m-login__sheet :deep(.van-cell::after) {
  border-color: var(--lc-line-soft);
}

.m-login__meta {
  padding: 14px 16px 18px;
}
.m-login__meta :deep(.van-checkbox__label) {
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-muted);
}

.m-login__submit {
  width: auto;
  margin: 0 16px;
  height: var(--lc-control-h);
  font-size: var(--lc-fs-lead);
  letter-spacing: 6px;
  border: none;
  border-radius: var(--lc-radius-ctl);
}
.m-login__submit:active {
  opacity: 0.9;
}

.m-login__foot {
  margin: 18px 16px 0;
  padding-top: 14px;
  border-top: 1px solid var(--lc-line-soft);
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-faint);
  text-align: center;
}

.m-login__version {
  margin: 26px 0 0;
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-faint);
  text-align: center;
}

@keyframes m-login-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
