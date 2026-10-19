<template>
  <!-- 403 无权限页：说明"为什么被拦"与"如何获得权限" -->
  <StatusPage
    code="403"
    kicker="ACCESS DENIED"
    title="没有访问权限"
    desc="当前账号缺少该页面所需的权限标识。请联系管理员在「角色管理 → 分配权限」中为你的角色勾选对应菜单或按钮，授权后重新登录或刷新即可生效。"
  >
    <el-button type="primary" @click="router.replace('/')">返回首页</el-button>
  </StatusPage>
</template>

<script setup>
/**
 * 403 无权限页
 * - 登录后访问未授权页面时由路由守卫进入
 * - 按钮级权限不足时后端返回 HTTP 403，前端在弹层提示错误信息
 */
import { useRouter } from 'vue-router'
import StatusPage from '@/components/StatusPage.vue'

const router = useRouter()
</script>
