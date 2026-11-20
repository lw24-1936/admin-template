<template>
  <el-dialog v-model="visible" title="两步验证（MFA）" width="520px" append-to-body>
    <div v-loading="loading">
      <template v-if="enabled">
        <el-alert type="success" :closable="false" show-icon title="两步验证已开启" />
        <p class="mfa-tip">关闭需要当前密码与认证器中的 6 位验证码。</p>
        <el-form label-width="96px">
          <el-form-item label="当前密码">
            <el-input v-model="password" type="password" show-password autocomplete="current-password" />
          </el-form-item>
          <el-form-item label="验证码">
            <el-input v-model="code" maxlength="6" placeholder="6 位数字" />
          </el-form-item>
        </el-form>
      </template>

      <template v-else>
        <el-alert type="info" :closable="false" show-icon
          title="使用任意 TOTP 认证器（如 Google Authenticator / 1Password）扫码或录入密钥" />
        <div v-if="secret" class="mfa-secret">
          <p class="mfa-tip">密钥（仅显示一次，请立即录入）：</p>
          <code>{{ secret }}</code>
          <p class="mfa-tip">otpauth URI：</p>
          <code class="mfa-uri">{{ otpauthUri }}</code>
        </div>
        <el-form label-width="96px">
          <el-form-item label="验证码">
            <el-input v-model="code" maxlength="6" placeholder="输入认证器中的 6 位数字" />
          </el-form-item>
        </el-form>
      </template>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button v-if="enabled" type="danger" :loading="saving" @click="handleDisable">关闭 MFA</el-button>
      <el-button v-else type="primary" :loading="saving" @click="handleEnable">开启 MFA</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { disableMfa, enableMfa, getMfaStatus, setupMfa } from '@/api/auth'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
const loading = ref(false)
const saving = ref(false)
const enabled = ref(false)
const secret = ref('')
const otpauthUri = ref('')
const code = ref('')
const password = ref('')

async function load() {
  loading.value = true
  try {
    const res = await getMfaStatus()
    enabled.value = Boolean(res.data?.enabled)
    if (!enabled.value) {
      const setup = await setupMfa()
      secret.value = setup.data?.secret || ''
      otpauthUri.value = setup.data?.otpauthUri || ''
    }
  } finally {
    loading.value = false
  }
}

watch(visible, (open) => {
  if (open) {
    code.value = ''
    password.value = ''
    secret.value = ''
    otpauthUri.value = ''
    load()
  }
})

async function handleEnable() {
  if (!/^\d{6}$/.test(code.value)) {
    ElMessage.warning('请输入 6 位验证码')
    return
  }
  saving.value = true
  try {
    await enableMfa({ code: code.value })
    ElMessage.success('两步验证已开启')
    enabled.value = true
    secret.value = ''
    otpauthUri.value = ''
  } finally {
    saving.value = false
  }
}

async function handleDisable() {
  if (!password.value || !/^\d{6}$/.test(code.value)) {
    ElMessage.warning('请输入当前密码与 6 位验证码')
    return
  }
  saving.value = true
  try {
    await disableMfa({ password: password.value, code: code.value })
    ElMessage.success('两步验证已关闭')
    enabled.value = false
    password.value = ''
    code.value = ''
    await load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.mfa-tip {
  margin: 10px 0 6px;
  color: var(--lc-ink-muted);
  font-size: 13px;
}
.mfa-secret code {
  display: block;
  padding: 8px;
  overflow-wrap: anywhere;
  font-size: 12px;
  background: var(--lc-surface-hover);
  border-radius: var(--lc-radius-ctl);
}
.mfa-uri {
  color: var(--lc-ink-muted);
}
</style>
