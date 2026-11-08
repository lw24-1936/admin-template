<template>
  <!-- 注册页：纸面极简排版 —— 无渐变、无光斑、无毛玻璃，
       用留白、字段标签与字重层级建立清晰而克制的视觉 -->
  <main class="reg-page">
    <div class="reg-sheet">
      <!-- 页眉：克制的小字 + 明确标题 -->
      <header class="reg-head">
        <p class="reg-kicker">admintem · 权限管理系统</p>
        <h1 class="reg-title">创建账号</h1>
        <p class="reg-sub">填写基本信息，创建后即可登录系统</p>
      </header>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @keyup.enter="handleRegister"
      >
        <el-row :gutter="18">
          <el-col :xs="24" :sm="12">
            <el-form-item label="账号" prop="userName">
              <el-input
                v-model="form.userName"
                placeholder="字母、数字或下划线"
                clearable
                autocomplete="username"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="系统内展示的名字" clearable />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12">
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                autocomplete="new-password"
                placeholder="至少 6 位"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                show-password
                autocomplete="new-password"
                placeholder="再次输入密码"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12">
            <el-form-item label="邮箱（选填）" prop="email">
              <el-input
                v-model="form.email"
                placeholder="用于找回密码"
                clearable
                autocomplete="email"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="手机号（选填）" prop="phone">
              <el-input
                v-model="form.phone"
                placeholder="便于联系与找回账号"
                clearable
                autocomplete="tel"
              />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="性别" prop="sex">
              <div class="sex-group" role="radiogroup" aria-label="性别">
                <label
                  v-for="option in sexOptions"
                  :key="option.value"
                  class="sex-item"
                  :class="{ 'is-active': form.sex === option.value }"
                >
                  <input
                    v-model="form.sex"
                    type="radio"
                    :value="option.value"
                    class="sex-radio"
                  />
                  <span class="sex-label">{{ option.label }}</span>
                </label>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-button class="reg-submit" :loading="loading" @click="handleRegister">
          创建账号
        </el-button>
      </el-form>

      <p class="reg-note">
        注册后将作为“普通用户”进入工作台，更多权限可由管理员在角色管理中分配。
      </p>

      <p class="reg-foot">
        已有账号？
        <button class="reg-link" type="button" @click="router.replace('/login')">返回登录</button>
      </p>
    </div>
  </main>
</template>

<script setup>
/**
 * 注册页（纸面极简排版）
 * 注册成功后回到登录页
 */
import { onMounted, reactive, ref } from 'vue'
import { animate, stagger } from 'animejs'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const sexOptions = [
  { value: 1, label: '男' },
  { value: 0, label: '女' },
]

/** 注册页入场动效：与登录页同一节奏（reduced-motion 下跳过） */
onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  animate('.reg-head > *', {
    opacity: [0, 1],
    y: [12, 0],
    duration: 460,
    delay: stagger(70),
    ease: 'outCubic',
  })
  animate('.reg-sheet', {
    opacity: [0, 1],
    y: [18, 0],
    scale: [0.988, 1],
    duration: 540,
    delay: 80,
    ease: 'outCubic',
  })
  animate('.reg-sheet .el-form-item, .sex-group, .reg-submit, .reg-note, .reg-foot', {
    opacity: [0, 1],
    y: [10, 0],
    duration: 360,
    delay: stagger(40, { start: 260 }),
    ease: 'outCubic',
  })
})

const form = reactive({
  userName: '',
  nickName: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  sex: 1,
})

/** 校验规则 */
const rules = {
  userName: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,32}$/, message: '4~32 位字母、数字或下划线', trigger: 'blur' },
  ],
  nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6~32 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
}

/** 提交注册 */
async function handleRegister() {
  // 校验失败时 Element Plus 会 reject，此处以返回值判断，避免未处理的 Promise rejection
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  // 校验为异步过程，先占位避免回车连击造成重复注册
  if (loading.value) return
  loading.value = true
  try {
    const payload = { ...form }
    delete payload.confirmPassword
    await register(payload)
    ElMessage.success('注册成功，请登录')
    router.replace('/login')
  } catch {
    // 错误提示已由拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 画布与登录页一致：柔和渐变 + 网格 + 主色光晕 */
.reg-page {
  position: relative;
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  padding: 40px 24px;
  overflow: hidden;
  background-image:
    radial-gradient(880px 420px at 8% -10%, rgba(255, 255, 255, 0.95), transparent 60%),
    radial-gradient(760px 420px at 100% 8%, var(--lc-accent-soft), transparent 62%),
    var(--lc-gradient-hero);
  font-family: var(--lc-font-sans);
}
.reg-page::before {
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

/* 玻璃表单卡 */
.reg-sheet {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 620px;
  padding: 38px 40px 30px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px) saturate(1.15);
  box-shadow: var(--lc-shadow-float);
}
.reg-head {
  margin-bottom: 24px;
}
.reg-kicker {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 12px;
  font-size: var(--lc-fs-kicker);
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--lc-ink-faint);
}
.reg-kicker::before {
  content: '';
  width: 24px;
  height: 1px;
  background: var(--lc-brand);
}
.reg-title {
  margin: 0 0 8px;
  font-size: var(--lc-fs-page);
  font-weight: 680;
  letter-spacing: -0.02em;
  color: var(--lc-ink);
}
.reg-sub {
  margin: 0;
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-muted);
}

/* 表单控件：与登录页同规格 */
.reg-sheet :deep(.el-form-item) {
  margin-bottom: 16px;
}
.reg-sheet :deep(.el-form-item__label) {
  height: auto;
  line-height: 1.4;
  margin-bottom: 6px;
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-body);
}
.reg-sheet :deep(.el-input__wrapper) {
  min-height: var(--lc-button-h);
  padding: 0 14px;
  background: var(--lc-paper-card);
  border-radius: var(--lc-radius-ctl);
  box-shadow: 0 0 0 1px var(--lc-line-input) inset;
  transition: box-shadow var(--lc-motion-fast) var(--lc-ease-out);
}
.reg-sheet :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--lc-line-hover) inset;
}
.reg-sheet :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px var(--lc-brand) inset, 0 0 0 4px var(--lc-brand-ring);
}

/* 性别选择：紧凑分段控件（3px 轨道 + 32px 选项，比输入框低一档） */
.sex-group {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  width: fit-content;
  padding: 3px;
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-ctl);
  background: var(--lc-surface-soft);
}
.sex-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 72px;
  height: 32px;
  padding: 0 16px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1;
  color: var(--lc-ink-muted);
  cursor: pointer;
  transition: color var(--lc-motion-fast), background-color var(--lc-motion-fast),
    box-shadow var(--lc-motion-fast);
}
.sex-item:hover {
  color: var(--lc-ink);
}
.sex-item.is-active {
  color: var(--lc-brand);
  font-weight: 600;
  background: var(--lc-paper-card);
  box-shadow: 0 1px 2px rgb(15 23 42 / 0.08), 0 0 0 1px var(--lc-line);
}
/* 选中态的小圆点，替代整块高亮，视觉更轻 */
.sex-item.is-active::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--lc-brand);
}
/* 原生 radio 只保留交互与可访问性，不显示 */
.sex-radio {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  opacity: 0;
  cursor: pointer;
}
.sex-item:has(.sex-radio:focus-visible) {
  outline: 2px solid var(--lc-brand);
  outline-offset: 2px;
}
.sex-label {
  pointer-events: none;
}

/* 主按钮：与登录页同款渐变 */
.reg-submit {
  width: 100%;
  height: var(--lc-button-h);
  margin-top: 6px;
  border: 1px solid var(--lc-brand-deep);
  border-radius: var(--lc-radius-ctl);
  font-size: var(--lc-fs-lead);
  font-weight: 600;
  letter-spacing: 0.28em;
  color: var(--lc-paper-card);
  background-color: var(--lc-brand);
  background-image: var(--lc-gradient-brand);
  box-shadow: 0 14px 30px -18px var(--lc-brand-glow);
  transition: transform var(--lc-motion-fast) var(--lc-ease-out),
    box-shadow var(--lc-motion-fast) var(--lc-ease-out), filter var(--lc-motion-fast);
}
.reg-submit:hover,
.reg-submit:focus {
  color: var(--lc-paper-card);
  filter: brightness(1.06);
  transform: translateY(-1px);
  box-shadow: 0 18px 36px -18px var(--lc-brand-glow);
}
.reg-submit:active {
  transform: translateY(0);
}

.reg-note {
  margin: 16px 0 0;
  font-size: var(--lc-fs-hint);
  line-height: 1.7;
  color: var(--lc-ink-faint);
  text-align: center;
}
.reg-foot {
  margin: 8px 0 0;
  padding-top: 14px;
  border-top: 1px solid var(--lc-line-soft);
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-muted);
  text-align: center;
}
.reg-link {
  border: none;
  background: none;
  padding: 0 4px;
  font-size: var(--lc-fs-label);
  color: var(--lc-brand);
  cursor: pointer;
}
.reg-link:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

@media (max-width: 640px) {
  .reg-page {
    padding: 22px 14px;
    align-items: flex-start;
  }
  .reg-sheet {
    padding: 26px 20px 22px;
    border-radius: var(--lc-radius-card);
  }
}
</style>
