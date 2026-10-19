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
              <el-input v-model="form.userName" placeholder="字母、数字或下划线" clearable autofocus />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="系统内展示的名字" clearable />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                show-password
                placeholder="再次输入密码"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12">
            <el-form-item label="邮箱（选填）" prop="email">
              <el-input v-model="form.email" placeholder="用于找回密码" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="手机号（选填）" prop="phone">
              <el-input v-model="form.phone" placeholder="便于联系与找回账号" clearable />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="性别" prop="sex">
              <div class="sex-group">
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
                  {{ option.label }}
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
import { reactive, ref } from 'vue'
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
/* ---------- 纸面基调：暖灰底、细边框、无重装饰 ---------- */
.reg-page {
  min-height: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  box-sizing: border-box;
  padding: 48px 16px 64px;
  background:
    linear-gradient(var(--lc-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--lc-grid-line) 1px, transparent 1px),
    var(--lc-paper);
  background-size: 32px 32px;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
}

.reg-sheet {
  width: 100%;
  max-width: 600px;
  background: var(--lc-paper-card);
  border: 1px solid var(--lc-line);
  border-radius: 14px;
  padding: 44px 48px 34px;
  box-shadow: var(--lc-shadow-sheet);
  animation: sheet-in 0.35s ease both;
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

/* ---------- 页眉排版 ---------- */
.reg-head {
  margin-bottom: 30px;
}
.reg-kicker {
  margin: 0 0 10px;
  font-size: 11px;
  letter-spacing: 2.4px;
  text-transform: uppercase;
  color: var(--lc-ink-faint);
}
.reg-title {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 650;
  letter-spacing: 0.2px;
  color: var(--lc-ink);
}
.reg-sub {
  margin: 0;
  font-size: 13px;
  color: var(--lc-ink-muted);
}

/* ---------- 字段：真正的 label + 清晰输入框 ---------- */
.reg-sheet :deep(.el-form-item) {
  margin-bottom: 18px;
}
.reg-sheet :deep(.el-form-item__label) {
  height: auto;
  line-height: 1.4;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--lc-ink-body);
}
.reg-sheet :deep(.el-form-item__content) {
  line-height: 1.4;
}

.reg-sheet :deep(.el-input__wrapper) {
  min-height: 44px;
  padding: 0 12px;
  background: var(--lc-paper-card);
  border-radius: 10px;
  box-shadow: 0 0 0 1px var(--lc-line-input) inset;
  transition: box-shadow 0.18s ease, background 0.18s ease;
}
.reg-sheet :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px var(--lc-brand) inset, 0 0 0 4px var(--lc-brand-ring);
}
.reg-sheet :deep(.el-input__inner) {
  color: var(--lc-ink);
}

/* 性别：极简单选按钮 */
.sex-group {
  display: flex;
  gap: 10px;
}
.sex-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 84px;
  height: 40px;
  padding: 0 18px;
  font-size: 14px;
  color: var(--lc-ink-body);
  background: var(--lc-paper);
  border: 1px solid var(--lc-line-strong);
  border-radius: 999px;
  cursor: pointer;
  user-select: none;
  transition: color 0.15s ease, background 0.15s ease, border-color 0.15s ease;
}
.sex-item.is-active {
  color: var(--lc-brand);
  background: var(--lc-brand-soft);
  border-color: var(--lc-brand);
  font-weight: 600;
}
.sex-radio {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

/* ---------- 主按钮：实色而非渐变 ---------- */
.reg-submit {
  width: 100%;
  height: 46px;
  margin-top: 6px;
  font-size: 15px;
  letter-spacing: 1px;
  color: var(--lc-paper-card);
  background: var(--lc-brand);
  border: none;
  border-radius: 10px;
  transition: background 0.18s ease, transform 0.12s ease;
}
.reg-submit:hover,
.reg-submit:focus {
  background: var(--lc-brand-hover);
}
.reg-submit:active {
  transform: translateY(1px);
}

/* ---------- 底部说明与链接 ---------- */
.reg-note {
  margin: 18px 0 0;
  font-size: 12px;
  color: var(--lc-ink-faint);
  text-align: center;
  line-height: 1.6;
}
.reg-foot {
  margin: 22px 0 0;
  padding-top: 16px;
  border-top: 1px solid var(--lc-line-soft);
  font-size: 13px;
  color: var(--lc-ink-muted);
  text-align: center;
}
.reg-link {
  border: none;
  background: none;
  padding: 0 4px;
  font-size: 13px;
  color: var(--lc-brand);
  cursor: pointer;
}
.reg-link:hover {
  text-decoration: underline;
}

/* ---------- 窄屏单列 ---------- */
@media (max-width: 640px) {
  .reg-page {
    padding: 24px 14px 48px;
  }
  .reg-sheet {
    padding: 30px 22px 26px;
  }
}
</style>
