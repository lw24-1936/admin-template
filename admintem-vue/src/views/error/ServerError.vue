<template>
  <StatusPage
    code="500"
    kicker="SYSTEM ERROR"
    title="系统开小差了"
    desc="页面渲染时发生了未处理的异常。可以刷新重试；如果反复出现，请把浏览器控制台错误和请求 traceId 交给管理员。"
  >
    <el-button type="primary" @click="retry">重新加载</el-button>
    <el-button @click="router.replace('/')">返回首页</el-button>
  </StatusPage>
</template>

<script setup>
/**
 * 500 兜底页：由 main.js 的 app.config.errorHandler 跳转进入。
 * 只负责展示与恢复动作，不吞掉控制台错误（便于定位）。
 */
import { useRouter } from 'vue-router'
import StatusPage from '@/components/StatusPage.vue'

const router = useRouter()

function retry() {
  window.location.reload()
}
</script>
