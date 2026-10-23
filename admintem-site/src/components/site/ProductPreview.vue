<template>
  <div class="relative overflow-hidden rounded-lg border border-border bg-card shadow-lift">
    <BorderBeam />

    <!-- 浏览器标题栏 -->
    <div class="flex items-center gap-2 border-b border-border bg-muted/50 px-4 py-2.5">
      <span class="flex gap-1.5" aria-hidden="true">
        <i class="h-2.5 w-2.5 rounded-full bg-[#f87171]" />
        <i class="h-2.5 w-2.5 rounded-full bg-[#fbbf24]" />
        <i class="h-2.5 w-2.5 rounded-full bg-[#34d399]" />
      </span>
      <span class="ml-2 truncate rounded-md bg-white/70 px-2 py-0.5 text-[11px] text-muted-foreground">
        nova.app / workspace / 产品发布
      </span>
    </div>

    <div class="grid grid-cols-[44px_1fr] sm:grid-cols-[52px_1fr]">
      <!-- 侧栏 -->
      <aside class="flex flex-col items-center gap-1 border-r border-border bg-muted/30 py-4" aria-hidden="true">
        <span class="grid h-7 w-7 place-items-center rounded-md bg-primary/10 text-primary">
          <LayoutGrid class="h-3.5 w-3.5" />
        </span>
        <span v-for="icon in sidebar" :key="icon" class="grid h-7 w-7 place-items-center rounded-md text-muted-foreground/60">
          <component :is="icon" class="h-3.5 w-3.5" />
        </span>
      </aside>

      <!-- 主区 -->
      <div class="min-w-0 p-4 sm:p-5">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="text-[10px] font-medium uppercase tracking-[0.16em] text-muted-foreground">Workspace</p>
            <p class="mt-0.5 text-sm font-semibold text-foreground">产品发布 · Q4</p>
          </div>
          <span class="rounded-full bg-emerald-500/10 px-2 py-0.5 text-[10px] font-medium text-emerald-700">
            进行中
          </span>
        </div>

        <div class="mt-4 grid grid-cols-2 gap-2.5">
          <div class="rounded-md border border-border bg-white/80 p-3">
            <p class="text-[10px] text-muted-foreground">本周完成</p>
            <p class="mt-1 text-xl font-bold text-foreground">18<span class="text-xs font-medium text-muted-foreground">/24</span></p>
            <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-muted">
              <div class="h-full w-3/4 rounded-full bg-primary" />
            </div>
          </div>
          <div class="rounded-md border border-border bg-white/80 p-3">
            <p class="text-[10px] text-muted-foreground">知识库条目</p>
            <p class="mt-1 text-xl font-bold text-foreground">1,284</p>
            <p class="mt-1.5 text-[10px] text-emerald-700">↑ 12% 较上周</p>
          </div>
        </div>

        <div class="mt-3 space-y-1.5">
          <div v-for="(task, i) in tasks" :key="task" class="flex items-center gap-2 rounded-md border border-border/70 bg-white/60 px-2.5 py-2">
            <span
              class="grid h-4 w-4 shrink-0 place-items-center rounded-full border"
              :class="i === 0 ? 'border-primary bg-primary text-white' : 'border-border text-transparent'"
              aria-hidden="true"
            >
              <Check class="h-2.5 w-2.5" />
            </span>
            <span class="truncate text-[11px] text-foreground/80">{{ task }}</span>
            <span class="ml-auto text-[10px] text-muted-foreground">{{ i === 0 ? '今天' : `${i + 1} 天` }}</span>
          </div>
        </div>

        <div class="mt-3 flex items-center gap-2 rounded-md border border-dashed border-border px-2.5 py-2 text-[11px] text-muted-foreground">
          <Sparkles class="h-3.5 w-3.5 text-primary" aria-hidden="true" />
          AI 摘要：本周发布节奏正常，风险集中在接口联调。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 产品预览（首屏右侧）
 *
 * 纯 DOM/CSS 绘制的"应用界面"，不依赖截图：换品牌后仍然成立，
 * 也不会成为 LCP 的图片资源。装饰图标全部 aria-hidden。
 */
import { Check, FileText, LayoutGrid, ListChecks, Search, Settings, Sparkles } from 'lucide-vue-next'
import BorderBeam from '@/components/magic/BorderBeam.vue'

const sidebar = [FileText, ListChecks, Search, Settings]
const tasks = ['发布说明初稿评审', '埋点字段确认', '灰度名单同步']
</script>
