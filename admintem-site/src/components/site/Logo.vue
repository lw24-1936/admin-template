<template>
  <div class="flex items-center gap-2.5">
    <span
      class="grid h-9 w-9 place-items-center rounded-[10px] bg-gradient-to-br from-primary to-[#7c3aed] text-primary-foreground shadow-soft"
      aria-hidden="true"
    >
      <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
        <path d="M5 18V6l14 12V6" />
      </svg>
    </span>
    <span class="flex flex-col leading-none">
      <span class="text-[17px] font-extrabold tracking-tight text-foreground">{{ site.name }}</span>
      <span class="mt-1 text-[10px] font-medium uppercase tracking-[0.18em] text-muted-foreground">
        Team workspace
      </span>
    </span>
  </div>
</template>

<script setup>
/** 品牌标记：图形 + 名称；换品牌只改 content/site.js 与这里的字形 */
import { site } from '@/content/site'
</script>
