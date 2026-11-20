<template>
  <main class="sso-page">
    <section class="sso-card">
      <p class="sso-kicker">ADMINTEM · SSO</p>
      <h1>正在完成单点登录</h1>
      <p v-if="loading">正在校验网关断言，请稍候…</p>
      <el-alert v-else-if="error" type="error" :closable="false" show-icon :title="error" />
      <p v-else>登录成功，正在进入系统…</p>
      <div class="sso-actions">
        <el-button v-if="error" type="primary" @click="router.replace('/login')">返回登录页</el-button>
      </div>
    </section>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ssoLogin } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  const assertion = route.query.ticket || route.query.assertion || route.query.token
  if (!assertion || typeof assertion !== 'string') {
    loading.value = false
    error.value = '缺少 SSO 票据，请从统一登录入口重新进入'
    return
  }
  try {
    const res = await ssoLogin({ assertion })
    await store.completeSsoLogin(res.data)
    const redirect = route.query.redirect
    router.replace(redirect && typeof redirect === 'string' ? redirect : '/')
  } catch (e) {
    error.value = 'SSO 登录失败：票据无效、已过期或账号未开通'
    console.error('[sso] 登录失败', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.sso-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--lc-gradient-hero);
  font-family: var(--lc-font-sans);
}
.sso-card {
  width: min(460px, 100%);
  padding: 32px;
  background: var(--lc-paper-card);
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-card);
  box-shadow: var(--lc-shadow-card);
}
.sso-kicker {
  margin: 0 0 8px;
  color: var(--lc-brand);
  font-size: 12px;
  letter-spacing: 0.16em;
}
h1 {
  margin: 0 0 12px;
  color: var(--lc-ink);
  font-size: 22px;
}
.sso-actions {
  margin-top: 16px;
}
</style>
