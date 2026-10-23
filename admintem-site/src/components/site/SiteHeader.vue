<template>
  <header class="sticky top-0 z-50 border-b border-border/70 bg-background/75 backdrop-blur-xl">
    <div class="container flex h-16 items-center justify-between gap-6">
      <RouterLink to="/" class="shrink-0 rounded-md" aria-label="回到首页">
        <Logo />
      </RouterLink>

      <nav class="hidden items-center gap-1 md:flex" aria-label="主导航">
        <RouterLink
          v-for="item in site.nav"
          :key="item.to"
          :to="item.to"
          class="rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted/60 hover:text-foreground"
          :class="isActive(item.to) ? 'bg-muted/70 text-foreground' : ''"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="hidden items-center gap-2 md:flex">
        <Button :href="site.actions.login.href" variant="ghost" size="sm">
          {{ site.actions.login.label }}
        </Button>
        <Button :to="site.actions.primary.to" size="sm">
          {{ site.actions.primary.label }}
          <ArrowRight class="h-4 w-4" aria-hidden="true" />
        </Button>
      </div>

      <button
        ref="menuButton"
        type="button"
        class="grid h-11 w-11 place-items-center rounded-lg border border-border text-foreground md:hidden"
        :aria-expanded="open"
        aria-controls="mobile-nav"
        :aria-label="open ? '关闭菜单' : '打开菜单'"
        @click="open = !open"
      >
        <X v-if="open" class="h-5 w-5" aria-hidden="true" />
        <Menu v-else class="h-5 w-5" aria-hidden="true" />
      </button>
    </div>

    <!-- 移动端菜单：Esc 关闭、打开时锁滚动、焦点进入面板 -->
    <div v-if="open" id="mobile-nav" ref="panel" class="border-t border-border bg-background md:hidden">
      <nav class="container flex flex-col gap-1 py-4" aria-label="移动端导航">
        <RouterLink
          v-for="item in site.nav"
          :key="item.to"
          :to="item.to"
          class="rounded-lg px-3 py-3 text-base font-medium text-foreground hover:bg-muted/60"
          @click="open = false"
        >
          {{ item.label }}
        </RouterLink>
        <div class="mt-3 flex flex-col gap-2">
          <Button :href="site.actions.login.href" variant="outline" block>登录</Button>
          <Button :to="site.actions.primary.to" block @click="open = false">
            {{ site.actions.primary.label }}
          </Button>
        </div>
      </nav>
    </div>
  </header>
</template>

<script setup>
/**
 * 站点顶栏：sticky 玻璃条 + 桌面导航 + 移动端折叠菜单。
 * 移动端菜单做到：aria-expanded/controls、Esc 关闭、锁滚动、焦点转移与归还。
 */
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ArrowRight, Menu, X } from 'lucide-vue-next'
import { site } from '@/content/site'
import Button from '@/components/ui/Button.vue'
import Logo from './Logo.vue'

const route = useRoute()
const open = ref(false)
const menuButton = ref(null)
const panel = ref(null)

function isActive(path) {
  return path === '/' ? route.path === '/' : route.path.startsWith(path)
}

watch(
  () => route.fullPath,
  () => {
    open.value = false
  }
)

watch(open, async (value) => {
  document.documentElement.style.overflow = value ? 'hidden' : ''
  if (value) {
    await nextTick()
    panel.value?.querySelector('a, button')?.focus()
  }
})

function onKeydown(event) {
  if (event.key === 'Escape' && open.value) {
    open.value = false
    menuButton.value?.focus()
  }
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  document.documentElement.style.overflow = ''
})
</script>
