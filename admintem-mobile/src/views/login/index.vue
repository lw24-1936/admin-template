<template>
  <!-- 移动端登录页：纸面极简（与桌面端同套视觉语言）
       单列窄卡、大号输入区、底部提示；无渐变与光晕 -->
  <main class="m-login">
    <header class="m-login__head">
      <!-- 品牌锁定：与桌面端侧边栏同一套标记（26px 主色方块 + 双行） -->
      <div class="m-login__brand">
        <img class="m-login__mark" src="/favicon.svg" alt="" width="28" height="28" />
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
            :type="showPassword ? 'text' : 'password'"
            name="password"
            label="密码"
            placeholder="请输入密码"
            autocomplete="current-password"
            :right-icon="showPassword ? 'eye-o' : 'closed-eye'"
            :rules="[{ required: true, message: '请输入密码' }]"
            @click-right-icon="showPassword = !showPassword"
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

      <p class="m-login__switch">
        还没有账号？
        <router-link to="/register" class="m-login__link">创建账号</router-link>
      </p>

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
const showPassword = ref(false)

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
/* 登录页：网格 + 两团柔光，与 uni-app/官网同一套浅色渐变语言 */
.m-login {
  position: relative;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 56px 16px calc(24px + env(safe-area-inset-bottom));
  background-color: var(--lc-paper);
  background-image:
    radial-gradient(circle at 88% -8%, rgba(47, 111, 237, 0.2), transparent 46%),
    radial-gradient(circle at -10% 32%, rgba(124, 58, 237, 0.12), transparent 42%),
    linear-gradient(var(--lc-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--lc-grid-line) 1px, transparent 1px);
  background-size: auto, auto, 28px 28px, 28px 28px;
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

/* 表单卡：半透明玻璃，避免纯白卡片压在渐变上太重 */
.m-login__sheet {
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(16px) saturate(1.05);
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

.m-login__switch {
  margin: 16px 16px 0;
  text-align: center;
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-muted);
}
.m-login__link {
  margin-left: 4px;
  color: var(--lc-brand);
  font-weight: 600;
  text-decoration: none;
}
.m-login__link:active {
  opacity: 0.7;
}

.m-login__foot {
  margin: 14px 16px 0;
  padding-top: 14px;
  border-top: 1px solid var(--lc-line-soft);
  text-align: center;
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

/* ===== V2 视觉覆盖（后置规则，优先级高于前面同名选择器） ===== */
/* 画布：渐变 + 光晕 + 网格（与桌面登录页同一语言） */
.m-login {
  position: relative;
  background-image:
    radial-gradient(520px 260px at 10% -8%, rgba(255, 255, 255, 0.95), transparent 62%),
    radial-gradient(420px 240px at 100% 6%, var(--lc-accent-soft), transparent 64%),
    var(--lc-gradient-hero);
  background-color: var(--lc-paper);
  overflow: hidden;
}
.m-login::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(var(--lc-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--lc-grid-line) 1px, transparent 1px);
  background-size: 26px 26px;
  mask-image: radial-gradient(120% 80% at 50% 0%, rgba(0, 0, 0, 0.8), transparent 76%);
}
.m-login > * { position: relative; z-index: 1; }

.m-login__mark {
  /* 品牌标记现在是一张 SVG（自带底色与白色字母），这里只保留统一的投影 */
  display: block;
  border-radius: 9px;
  box-shadow: 0 8px 16px -12px var(--lc-brand-glow);
}
.m-login__title {
  font-weight: 680;
  letter-spacing: -0.02em;
}
/* 表单卡：玻璃 + 大圆角 + 悬浮投影 */
.m-login__sheet {
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(14px) saturate(1.1);
  box-shadow: var(--lc-shadow-float);
  padding: 18px 16px 16px;
}
.m-login__sheet :deep(.van-cell-group--inset) {
  margin: 0;
  border-radius: var(--lc-radius-ctl);
  background: var(--lc-paper-card);
  box-shadow: 0 0 0 1px var(--lc-line) inset;
}
.m-login__submit {
  height: 46px;
  margin-top: 6px;
  border: 1px solid var(--lc-brand-deep);
  border-radius: var(--lc-radius-ctl);
  background-color: var(--lc-brand);
  background-image: var(--lc-gradient-brand);
  box-shadow: 0 12px 26px -16px var(--lc-brand-glow);
  letter-spacing: 0.28em;
  font-weight: 600;
}
.m-login__foot {
  color: var(--lc-ink-faint);
}

/* ===== V2 登录页：渐变画布 + 玻璃表单卡（与桌面/uni-app 同一语言） ===== */
.m-login {
  position: relative;
  overflow: hidden;
  background-color: var(--lc-paper);
  background-image: radial-gradient(420px 240px at 8% -10%, rgba(255, 255, 255, 0.95), transparent 62%),
    linear-gradient(180deg, #f8fafc 0%, #eef3fb 54%, #e7eef9 100%);
}
.m-login::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: linear-gradient(var(--lc-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--lc-grid-line) 1px, transparent 1px);
  background-size: 28px 28px;
  mask-image: radial-gradient(120% 80% at 50% 0%, rgba(0, 0, 0, 0.8), transparent 76%);
}
.m-login > * {
  position: relative;
  z-index: 1;
}
.m-login__head {
  margin-bottom: 26px;
}
.m-login__brand {
  margin-bottom: 26px;
}
.m-login__mark {
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  display: block;
  border-radius: 12px;
  box-shadow: 0 10px 22px -14px var(--lc-brand-glow);
}
.m-login__brand-text strong {
  font-size: 16px;
  letter-spacing: -0.01em;
}
.m-login__brand-text em {
  font-size: 11px;
  letter-spacing: 0.14em;
}
.m-login__title {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--lc-ink);
}
.m-login__desc {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--lc-ink-muted);
}
.m-login__sheet {
  padding: 20px 18px 16px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px) saturate(1.15);
  box-shadow: var(--lc-shadow-float);
}
.m-login__sheet :deep(.van-cell-group--inset) {
  margin: 0;
  overflow: hidden;
  border-radius: var(--lc-radius-ctl);
  background: var(--lc-paper-card);
  box-shadow: 0 0 0 1px var(--lc-line) inset;
}
.m-login__sheet :deep(.van-field__label) {
  color: var(--lc-ink-body);
  font-size: 13px;
}
.m-login__meta {
  margin: 14px 0 18px;
}
.m-login__submit {
  width: 100%;
  height: 48px;
  border: 1px solid var(--lc-brand-deep);
  border-radius: var(--lc-radius-ctl);
  background-color: var(--lc-brand);
  background-image: var(--lc-gradient-brand);
  box-shadow: 0 14px 30px -18px var(--lc-brand-glow);
  font-weight: 600;
  letter-spacing: 0.28em;
}
.m-login__foot {
  color: var(--lc-ink-faint);
  font-size: 12px;
}
.m-login__version {
  color: var(--lc-ink-faint);
}
</style>
