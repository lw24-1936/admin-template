<template>
  <!-- 登录页：纸面极简（与注册页同套语言）
       左侧为编辑式品牌排版，右侧为白卡片表单；无渐变、无光斑 -->
  <main class="login-page">
    <div class="login-locale"><LocaleSwitch /></div>
      <!-- 指针跟随的柔光（纯装饰层；触摸设备与 reduced-motion 下不启用） -->
      <div class="login-aurora" aria-hidden="true" />
    <div class="login-grid">
      <!-- 左：品牌文案排版 -->
      <section class="login-copy">
        <p class="login-kicker">admintem · 权限管理系统</p>
        <h1 class="login-title">
          集中管理身份
          <br />
          与访问权限
        </h1>
        <p class="login-desc">
          菜单与按钮由同一套权限标识驱动，前后端双重校验，
          让每一次访问都有据可循。
        </p>

        <ul class="login-points">
          <li>权限标识驱动菜单与按钮</li>
          <li>令牌存入 Redis，可即时失效</li>
          <li>页面由数据库菜单动态注册</li>
        </ul>

        <p class="login-version">admintem v1.0 · dev</p>
      </section>

      <!-- 右：登录表单 -->
      <section class="login-form-side">
        <div class="login-sheet">
          <header class="login-sheet-head">
            <h2>登录</h2>
            <p>{{ t('login.subtitle') }}</p>
          </header>

          <el-form
              v-if="!mfaRequired"
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            size="large"
            @keyup.enter="handleLogin"
          >
            <el-form-item :label="t('login.username')" prop="username">
              <el-input
                v-model="form.username"
                placeholder="admin"
                clearable
                autofocus
                autocomplete="username"
              />
            </el-form-item>
            <el-form-item :label="t('login.password')" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                :placeholder="t('login.passwordPlaceholder')"
                autocomplete="current-password"
                show-password
              />
            </el-form-item>

            <el-form-item label="验证码">
              <CodeCaptcha ref="captchaRef" @success="onCaptchaSuccess" />
            </el-form-item>

            <div class="login-meta">
              <el-checkbox v-model="form.rememberMe">{{ t('login.remember') }}</el-checkbox>
            </div>

            <el-button class="login-submit" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form>

            <div v-if="mfaRequired" class="login-mfa">
              <h3>{{ t('login.mfaTitle') }}</h3>
              <p>{{ t('login.mfaHint') }}</p>
              <el-input v-model="mfaCode" maxlength="6" size="large"
                :placeholder="t('login.mfaCode')" @keyup.enter="handleVerifyMfa" />
              <el-button class="login-submit" :loading="mfaLoading" @click="handleVerifyMfa">
                {{ t('login.mfaSubmit') }}
              </el-button>
              <button class="login-link" type="button" @click="cancelMfa">
                {{ t('login.mfaCancel') }}
              </button>
            </div>

            <p v-if="!mfaRequired" class="login-foot">
              <button class="login-link" type="button" @click="forgotVisible = true">{{ t('login.forgot') }}</button>
              <span class="login-sep">·</span>
              {{ t('login.noAccount') }}
            <button class="login-link" type="button" @click="router.push('/register')">
                {{ t('login.createAccount') }}
            </button>
          </p>
        </div>
      </section>
    </div>
      <ForgotPasswordDialog v-model="forgotVisible" />
  </main>
</template>

<script setup>
/**
 * 登录页（纸面极简排版，与注册页同套视觉）
 * - 支持记住我（延长令牌有效期）
 * - 登录成功后回跳 redirect 或首页
 */
import { onMounted, reactive, ref } from 'vue'
import { animate, stagger } from 'animejs'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import CodeCaptcha from '@/components/CodeCaptcha.vue'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { useI18n } from '@/i18n'
import { verifyMfa } from '@/api/auth'
import ForgotPasswordDialog from '@/components/ForgotPasswordDialog.vue'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const formRef = ref()
const loading = ref(false)
const mfaRequired = ref(false)
const mfaToken = ref('')
const mfaCode = ref('')
const mfaLoading = ref(false)
const forgotVisible = ref(false)
const captchaRef = ref(null)
const captchaTicket = ref('')

/**
 * 登录页入场（anime.js v4）。
 *
 * 节奏：文案 → 表单卡 → 字段逐个淡入，总时长 ≤ 900ms。
 * reduced-motion 下直接跳过（不预先隐藏元素，JS 失败也能看到完整页面）。
 */
onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  animate('.login-kicker, .login-title, .login-desc, .login-points, .login-version', {
    opacity: [0, 1],
    y: [14, 0],
    duration: 520,
    delay: stagger(70),
    // 文案做轻微 blur-fade：不碰带 backdrop-filter 的表单卡，避免玻璃效果失效
    filter: ['blur(5px)', 'blur(0px)'],
    ease: 'outCubic',
  })
  animate('.login-sheet', {
    opacity: [0, 1],
    y: [18, 0],
    scale: [0.985, 1],
    duration: 560,
    delay: 140,
    ease: 'outCubic',
  })
  animate('.login-sheet .el-form-item, .login-meta, .login-submit, .login-foot', {
    opacity: [0, 1],
    y: [10, 0],
    duration: 380,
    delay: stagger(46, { start: 320 }),
    ease: 'outCubic',
  })
})

/**
 * 指针跟随柔光（Magic UI「magic-card」的 CSS 复刻，不引入 React 与 Tailwind）。
 *
 * 只写两个自定义属性 --mx/--my，由 CSS 的 radial-gradient 消费；
 * 用 requestAnimationFrame 合并高频 pointermove，避免每个事件都触发样式重算。
 * 触摸设备（hover: none）与 reduced-motion 下不挂监听——它们不需要跟随光斑。
 */
onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  if (window.matchMedia('(hover: none)').matches) return
  const page = document.querySelector('.login-page')
  if (!page) return
  let raf = 0
  let px = 0
  let py = 0
  const apply = () => {
    raf = 0
    page.style.setProperty('--mx', px + 'px')
    page.style.setProperty('--my', py + 'px')
  }
  page.addEventListener('pointermove', (event) => {
    px = event.clientX
    py = event.clientY
    if (!raf) raf = requestAnimationFrame(apply)
  })
})

const form = reactive({
  username: 'admin',
  password: '12345678',
  rememberMe: false,
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function onCaptchaSuccess(ticket) {
  captchaTicket.value = ticket
}

/** 提交登录 */
async function handleLogin() {
  // 校验失败时 Element Plus 会 reject，此处以返回值判断，避免未处理的 Promise rejection
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  // 表单校验是异步的，先占位可避免回车连击造成重复登录请求
  if (loading.value) return
  loading.value = true
  try {
    await store.login({ ...form, captchaTicket: captchaTicket.value })
    ElMessage.success('登录成功')
    // 回跳来源页，默认进入工作台
    const redirect = route.query.redirect
    router.replace(redirect && typeof redirect === 'string' ? redirect : '/')
  } catch {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    loading.value = false
  }
}

  async function handleVerifyMfa() {
    if (!/^\d{6}$/.test(mfaCode.value)) {
      ElMessage.warning('请输入 6 位验证码')
      return
    }
    if (mfaLoading.value) return
    mfaLoading.value = true
    try {
      const res = await verifyMfa({ mfaToken: mfaToken.value, code: mfaCode.value })
      await store.completeMfaLogin(res.data)
      ElMessage.success('登录成功')
      const redirect = route.query.redirect
      router.replace(redirect && typeof redirect === 'string' ? redirect : '/')
    } catch {
      // 错误提示由 request 拦截器统一处理
    } finally {
      mfaLoading.value = false
    }
  }

  function cancelMfa() {
    mfaRequired.value = false
    mfaToken.value = ''
    mfaCode.value = ''
  }
</script>

<style scoped>
/* ---------- 画布：柔和渐变 + 极淡网格 + 两团主色光晕 ---------- */
.login-page {
  position: relative;
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  padding: 48px 24px;
  overflow: hidden;
  background-image:
    radial-gradient(880px 420px at 8% -10%, rgba(255, 255, 255, 0.95), transparent 60%),
    radial-gradient(760px 420px at 100% 8%, var(--lc-accent-soft), transparent 62%),
    var(--lc-gradient-hero);
  font-family: var(--lc-font-sans);
}
/* 网格与光斑都是装饰层：fixed/absolute + pointer-events:none，不参与布局 */
.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(var(--lc-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--lc-grid-line) 1px, transparent 1px);
  background-size: 30px 30px;
  mask-image: radial-gradient(120% 90% at 50% 0%, rgba(0, 0, 0, 0.85), transparent 78%);
}
.login-page::after {
  content: '';
  position: absolute;
  width: 520px;
  height: 520px;
  right: -140px;
  bottom: -220px;
  pointer-events: none;
  border-radius: 50%;
  background: radial-gradient(circle, var(--lc-brand-glow) 0%, transparent 62%);
  filter: blur(6px);
}

.login-aurora {
  position: absolute;
  inset: 0;
  pointer-events: none;
  /* 跟随指针的柔光：位置由 --mx/--my 决定，未移动时落在左上 */
  background: radial-gradient(
    340px 340px at var(--mx, 32%) var(--my, 30%),
    var(--lc-brand-glow) 0%,
    transparent 68%
  );
  opacity: 0.55;
}

.login-grid {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 1060px;
  display: grid;
  grid-template-columns: 1.08fr 1fr;
  gap: 80px;
  align-items: center;
}

/* ---------- 左：品牌文案 ---------- */
.login-copy {
  max-width: 470px;
}
.login-kicker {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 22px;
  font-size: var(--lc-fs-kicker);
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--lc-ink-faint);
}
.login-kicker::before {
  content: '';
  width: 26px;
  height: 1px;
  background: var(--lc-brand);
}
.login-title {
  margin: 0 0 22px;
  font-size: var(--lc-fs-display);
  line-height: 1.14;
  font-weight: 680;
  letter-spacing: -0.025em;
  color: var(--lc-ink);
}
.login-desc {
  margin: 0;
  max-width: 400px;
  font-size: var(--lc-fs-body);
  line-height: 1.9;
  color: var(--lc-ink-soft);
}

.login-points {
  margin: 32px 0 0;
  padding: 24px 0 0;
  border-top: 1px solid var(--lc-line-strong);
  list-style: none;
}
.login-points li {
  position: relative;
  padding: 8px 0 8px 22px;
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-body);
}
/* 指示点：主色实心 + 外圈光晕，替代默认圆点 */
.login-points li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 15px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--lc-brand);
  box-shadow: 0 0 0 4px var(--lc-brand-ring);
}

.login-version {
  margin: 36px 0 0;
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-faint);
}

/* ---------- 右：玻璃表单卡 ---------- */
.login-form-side {
  display: flex;
  justify-content: flex-end;
}
.login-sheet {
  width: 100%;
  max-width: 428px;
  padding: 40px 38px 30px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(16px) saturate(1.15);
  box-shadow: var(--lc-shadow-float);
}

.login-sheet-head {
  margin-bottom: 26px;
}
.login-sheet-head h2 {
  margin: 0 0 6px;
  font-size: var(--lc-fs-title);
  font-weight: 650;
  letter-spacing: -0.015em;
  color: var(--lc-ink);
}
.login-sheet-head p {
  margin: 0;
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-muted);
}

/* 字段：标签在上、输入框 46px，与移动端手感一致 */
.login-sheet :deep(.el-form-item) {
  margin-bottom: 18px;
}
.login-sheet :deep(.el-form-item__label) {
  height: auto;
  line-height: 1.4;
  margin-bottom: 6px;
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-body);
}
.login-sheet :deep(.el-input__wrapper) {
  min-height: var(--lc-button-h);
  padding: 0 14px;
  background: var(--lc-paper-card);
  border-radius: var(--lc-radius-ctl);
  box-shadow: 0 0 0 1px var(--lc-line-input) inset;
  transition: box-shadow var(--lc-motion-fast) var(--lc-ease-out);
}
.login-sheet :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--lc-line-hover) inset;
}
.login-sheet :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px var(--lc-brand) inset, 0 0 0 4px var(--lc-brand-ring);
}
.login-sheet :deep(.el-input__inner) {
  color: var(--lc-ink);
}

.login-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
}
.login-meta :deep(.el-checkbox__label) {
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-muted);
}

/* 主按钮：深蓝渐变 + 外发光 + 悬停上浮 */
.login-submit {
  width: 100%;
  height: var(--lc-button-h);
  border: 1px solid var(--lc-brand-deep);
  border-radius: var(--lc-radius-ctl);
  font-size: var(--lc-fs-lead);
  font-weight: 600;
  letter-spacing: 0.32em;
  color: var(--lc-paper-card);
  background-color: var(--lc-brand);
  background-image: var(--lc-gradient-brand);
  box-shadow: 0 14px 30px -18px var(--lc-brand-glow);
  transition: transform var(--lc-motion-fast) var(--lc-ease-out),
    box-shadow var(--lc-motion-fast) var(--lc-ease-out), filter var(--lc-motion-fast);
}
.login-submit:hover,
.login-submit:focus {
  color: var(--lc-paper-card);
  filter: brightness(1.06);
  transform: translateY(-1px);
  box-shadow: 0 18px 36px -18px var(--lc-brand-glow);
}
.login-submit:active {
  transform: translateY(0);
}

.login-foot {
  margin: 22px 0 0;
  padding-top: 16px;
  border-top: 1px solid var(--lc-line-soft);
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-muted);
  text-align: center;
}
.login-link {
  border: none;
  background: none;
  padding: 0 4px;
  font-size: var(--lc-fs-label);
  color: var(--lc-brand);
  cursor: pointer;
}
.login-link:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

/* ---------- 窄屏：单列、仅表单 ---------- */
@media (max-width: 900px) {
  .login-grid {
    grid-template-columns: 1fr;
    gap: 30px;
    max-width: 440px;
  }
  .login-copy {
    max-width: none;
    text-align: center;
  }
  .login-kicker {
    justify-content: center;
  }
  .login-title {
    font-size: 30px;
  }
  .login-desc {
    margin: 0 auto;
  }
  .login-points,
  .login-version {
    display: none;
  }
  .login-form-side {
    justify-content: center;
  }
}
@media (max-width: 480px) {
  .login-page {
    padding: 26px 14px;
  }
  .login-sheet {
    padding: 30px 22px 26px;
    border-radius: var(--lc-radius-card);
  }
}
</style>

<style scoped>
.login-locale {
  position: fixed;
  top: 20px;
  right: 24px;
  z-index: 10;
}
</style>

<style scoped>
.login-mfa {
  margin-top: 12px;
}
.login-mfa h3 {
  margin: 0 0 8px;
}
.login-mfa p {
  margin: 0 0 12px;
  color: var(--lc-ink-muted);
  font-size: 13px;
}
.login-mfa .login-submit {
  width: 100%;
  margin-top: 14px;
}
.login-mfa .login-link {
  margin-top: 10px;
}
</style>

<style scoped>
.login-sep {
  margin: 0 6px;
  color: var(--lc-ink-soft);
}
</style>
