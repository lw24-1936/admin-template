<template>
  <footer class="border-t border-border bg-card/60">
    <div class="container py-14">
      <div class="grid gap-10 md:grid-cols-[1.4fr_1fr_1fr_1fr]">
        <div>
          <Logo />
          <p class="mt-4 max-w-xs text-sm leading-6 text-muted-foreground">
            {{ site.description }}
          </p>
          <a
            :href="`mailto:${site.contact.email}`"
            class="mt-4 inline-block text-sm font-medium text-primary hover:underline"
          >
            {{ site.contact.email }}
          </a>
        </div>

        <div v-for="group in groups" :key="group.title">
          <h3 class="text-sm font-semibold text-foreground">{{ group.title }}</h3>
          <ul class="mt-4 space-y-2.5">
            <li v-for="link in group.links" :key="link.label">
              <RouterLink
                :to="link.to"
                class="text-sm text-muted-foreground transition-colors hover:text-foreground"
              >
                {{ link.label }}
              </RouterLink>
            </li>
          </ul>
        </div>
      </div>

      <div class="mt-12 flex flex-col gap-3 border-t border-border pt-6 sm:flex-row sm:items-center sm:justify-between">
        <p class="text-xs text-muted-foreground">
          © {{ year }} {{ site.name }}. 保留所有权利。
        </p>
        <p class="text-xs text-muted-foreground">
          Vue 3 · Tailwind CSS · anime.js
        </p>
      </div>
    </div>
  </footer>
</template>

<script setup>
/** 页脚：品牌 + 三组链接 + 版权；链接全部指向站内有效路由，避免死链 */
import { RouterLink } from 'vue-router'
import { site } from '@/content/site'
import Logo from './Logo.vue'

const year = new Date().getFullYear()

const groups = [
  {
    title: '产品',
    links: [
      { label: '功能总览', to: '/features' },
      { label: '价格方案', to: '/pricing' },
      { label: '更新日志', to: '/features' },
      { label: '集成', to: '/features' },
    ],
  },
  {
    title: '公司',
    links: [
      { label: '关于我们', to: '/about' },
      { label: '联系我们', to: '/contact' },
      { label: '加入我们', to: '/about' },
    ],
  },
  {
    title: '资源',
    links: [
      { label: '帮助中心', to: '/contact' },
      { label: '隐私政策', to: '/about' },
      { label: '服务条款', to: '/about' },
    ],
  },
]
</script>
