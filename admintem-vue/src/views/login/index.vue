<template>
  <!-- 登录页：纸面极简（与注册页同套语言）
       左侧为编辑式品牌排版，右侧为白卡片表单；无渐变、无光斑 -->
  <main class="login-page">
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
            <p>使用您的账号继续访问</p>
          </header>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            size="large"
            @keyup.enter="handleLogin"
          >
            <el-form-item label="账号" prop="username">
              <el-input v-model="form.username" placeholder="admin" clearable autofocus />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                show-password
              />
            </el-form-item>

            <div class="login-meta">
              <el-checkbox v-model="form.rememberMe">记住我（7 天内免登录）</el-checkbox>
            </div>

            <el-button class="login-submit" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form>

          <p class="login-foot">
            还没有账号？
            <button class="login-link" type="button" @click="router.push('/register')">
              创建账号
            </button>
          </p>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup>
/**
 * 登录页（纸面极简排版，与注册页同套视觉）
 * - 支持记住我（延长令牌有效期）
 * - 登录成功后回跳 redirect 或首页
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '12345678',
  rememberMe: false,
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
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
    await store.login({ ...form })
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
</script>

<style scoped>
/* ---------- 纸面基调（与注册页一致） ---------- */
.login-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  padding: 40px 24px;
  background:
    linear-gradient(var(--lc-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--lc-grid-line) 1px, transparent 1px),
    var(--lc-paper);
  background-size: 32px 32px;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
}

.login-grid {
  width: 100%;
  max-width: 1020px;
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 72px;
  align-items: center;
}

/* ---------- 左：文案排版 ---------- */
.login-copy {
  animation: copy-in 0.4s ease both;
}
.login-kicker {
  margin: 0 0 18px;
  font-size: 11px;
  letter-spacing: 2.6px;
  text-transform: uppercase;
  color: var(--lc-ink-faint);
}
.login-title {
  margin: 0 0 20px;
  font-size: 40px;
  line-height: 1.22;
  font-weight: 650;
  letter-spacing: 0.5px;
  color: var(--lc-ink);
}
.login-desc {
  margin: 0;
  max-width: 380px;
  font-size: 14px;
  line-height: 1.9;
  color: var(--lc-ink-soft);
}

.login-points {
  margin: 30px 0 0;
  padding: 22px 0 0;
  border-top: 1px solid var(--lc-line-strong);
  list-style: none;
}
.login-points li {
  position: relative;
  padding: 7px 0 7px 18px;
  font-size: 13px;
  color: var(--lc-ink-body);
}
.login-points li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 15px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--lc-brand);
}

.login-version {
  margin: 34px 0 0;
  font-size: 12px;
  color: var(--lc-ink-faint);
}

/* ---------- 右：登录表单 ---------- */
.login-form-side {
  display: flex;
  justify-content: flex-end;
}
.login-sheet {
  width: 100%;
  max-width: 420px;
  background: var(--lc-paper-card);
  border: 1px solid var(--lc-line);
  border-radius: 14px;
  padding: 40px 40px 30px;
  box-shadow: var(--lc-shadow-sheet);
  animation: sheet-in 0.4s ease both;
}
@keyframes copy-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@keyframes sheet-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-sheet-head {
  margin-bottom: 26px;
}
.login-sheet-head h2 {
  margin: 0 0 6px;
  font-size: 23px;
  font-weight: 650;
  color: var(--lc-ink);
}
.login-sheet-head p {
  margin: 0;
  font-size: 13px;
  color: var(--lc-ink-muted);
}

/* 字段标签 + 输入框（与注册页一致） */
.login-sheet :deep(.el-form-item) {
  margin-bottom: 18px;
}
.login-sheet :deep(.el-form-item__label) {
  height: auto;
  line-height: 1.4;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--lc-ink-body);
}
.login-sheet :deep(.el-input__wrapper) {
  min-height: 44px;
  padding: 0 12px;
  background: var(--lc-paper-card);
  border-radius: 10px;
  box-shadow: 0 0 0 1px var(--lc-line-input) inset;
  transition: box-shadow 0.18s ease;
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
  font-size: 13px;
  color: var(--lc-ink-muted);
}

/* 主按钮：实色（无渐变） */
.login-submit {
  width: 100%;
  height: 46px;
  font-size: 15px;
  letter-spacing: 6px;
  color: var(--lc-paper-card);
  background: var(--lc-brand);
  border: none;
  border-radius: 10px;
  transition: background 0.18s ease, transform 0.12s ease;
}
.login-submit:hover,
.login-submit:focus {
  background: var(--lc-brand-hover);
}
.login-submit:active {
  transform: translateY(1px);
}

.login-foot {
  margin: 22px 0 0;
  padding-top: 16px;
  border-top: 1px solid var(--lc-line-soft);
  font-size: 13px;
  color: var(--lc-ink-muted);
  text-align: center;
}
.login-link {
  border: none;
  background: none;
  padding: 0 4px;
  font-size: 13px;
  color: var(--lc-brand);
  cursor: pointer;
}
.login-link:hover {
  text-decoration: underline;
}

/* ---------- 窄屏：单列、仅表单 ---------- */
@media (max-width: 860px) {
  .login-grid {
    grid-template-columns: 1fr;
    gap: 32px;
    max-width: 440px;
  }
  .login-copy {
    text-align: center;
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
  .login-title {
    font-size: 30px;
  }
}
@media (max-width: 480px) {
  .login-page {
    padding: 24px 14px;
  }
  .login-sheet {
    padding: 30px 22px 26px;
  }
}
</style>
