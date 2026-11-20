<template>
  <!-- 404：区分"地址写错"与"菜单未授权导致路由未注册"两种常见原因 -->
  <StatusPage
    code="404"
    kicker="NOT FOUND"
    :title="t('error.404.title', '页面不存在')"
    :desc="t('error.404.desc', '地址有误，或该菜单未授权给你的角色——未授权的菜单不会注册路由，因此直达地址会落到这里。请从左侧菜单进入，或联系管理员确认菜单授权。')"
  >
    <el-button type="primary" @click="router.replace('/')">{{ t('common.home') }}</el-button>
  </StatusPage>
</template>

<script setup>
/**
 * 404 页面不存在
 * - 由动态路由之后的兜底路由渲染
 */
import { useRouter } from 'vue-router'
import StatusPage from '@/components/StatusPage.vue'
import { useI18n } from '@/i18n'

const { t } = useI18n()

const router = useRouter()
</script>
