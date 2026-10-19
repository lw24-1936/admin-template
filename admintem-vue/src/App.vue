<template>
  <!-- 根组件：仅承载路由出口
       Element Plus 按需引入后，中文语言包不再通过 app.use(ElementPlus, { locale }) 设置，
       改由 ElConfigProvider 提供（影响分页、弹窗按钮、日期等内置文案） -->
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup>
/**
 * 应用根组件
 * - 提供 Element Plus 中文语言包（zh-cn）
 * - 页面内容由 <router-view /> 渲染
 */
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>
