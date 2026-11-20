<template>
  <el-dialog v-model="visible" title="找回密码" width="440px" append-to-body>
    <el-alert type="info" :closable="false" show-icon
      title="验证码 10 分钟内有效；若账号不存在，接口不会提示，以防账号枚举。" />
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" style="margin-top: 14px">
      <el-form-item label="账号" prop="userName">
        <el-input v-model="form.userName" autocomplete="username" />
      </el-form-item>
      <el-form-item label="验证码" prop="code">
        <div class="code-row">
          <el-input v-model="form.code" maxlength="6" autocomplete="one-time-code" />
          <el-button :disabled="countdown > 0" :loading="sending" @click="handleSend">
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleReset">重置密码</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { requestPasswordReset, resetPassword } from '@/api/auth'

const props = defineProps({ modelValue: { type: Boolean, default: false } })
const emit = defineEmits(['update:modelValue', 'success'])
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
const formRef = ref()
const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
let timer = null
const form = reactive({ userName: '', code: '', newPassword: '', confirmPassword: '' })
const rules = {
  userName: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  code: [{ required: true, pattern: /^\d{6}$/, message: '请输入 6 位验证码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '新密码至少 6 位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) =>
        value === form.newPassword ? callback() : callback(new Error('两次输入的密码不一致')),
      trigger: 'blur',
    },
  ],
}

watch(visible, (open) => {
  if (open) {
    form.userName = ''
    form.code = ''
    form.newPassword = ''
    form.confirmPassword = ''
    countdown.value = 0
  }
})

watch(countdown, (value) => {
  if (timer) clearInterval(timer)
  if (value <= 0) return
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
})

async function handleSend() {
  const valid = await formRef.value.validateField('userName').catch(() => false)
  if (valid === false) return
  sending.value = true
  try {
    await requestPasswordReset({ userName: form.userName })
    ElMessage.success('如果账号存在，验证码已发送')
    countdown.value = 60
  } finally {
    sending.value = false
  }
}

async function handleReset() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await resetPassword({
      userName: form.userName,
      code: form.code,
      newPassword: form.newPassword,
    })
    ElMessage.success('密码已重置，请重新登录')
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.code-row .el-input {
  flex: 1;
}
</style>
