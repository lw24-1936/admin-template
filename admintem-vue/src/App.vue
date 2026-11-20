<template>
  <!-- 根组件：仅承载路由出口；Element Plus locale 随应用语言切换 -->
  <el-config-provider :locale="elementLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import { useI18n } from '@/i18n'

const { locale } = useI18n()
const elementLocale = computed(() => (locale.value === 'en-US' ? en : zhCn))
</script>
