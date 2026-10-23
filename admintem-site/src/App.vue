<template>
  <a
    href="#main"
    class="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[100] focus:rounded-lg focus:bg-primary focus:px-4 focus:py-2 focus:text-primary-foreground"
  >
    跳到主要内容
  </a>
  <SiteHeader />
  <main id="main" class="min-h-[60vh]">
    <RouterView v-slot="{ Component }">
      <component :is="Component" />
    </RouterView>
  </main>
  <SiteFooter />
</template>

<script setup>
/**
 * 站点外壳：跳过导航链接 + 顶栏 + 路由出口 + 页脚。
 * 页面本身只关心内容，导航与页脚在路由之间保持不变（不重新挂载）。
 */
import SiteHeader from '@/components/site/SiteHeader.vue'
import SiteFooter from '@/components/site/SiteFooter.vue'
</script>
