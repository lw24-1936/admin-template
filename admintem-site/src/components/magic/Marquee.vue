<template>
  <div class="mask-fade-x overflow-hidden" role="presentation">
    <div class="flex w-max animate-marquee gap-12 pr-12 motion-reduce:animate-none hover:[animation-play-state:paused]">
      <ul class="flex items-center gap-12">
        <li
          v-for="logo in logos"
          :key="logo"
          class="text-lg font-bold tracking-tight text-muted-foreground/70 transition-colors hover:text-foreground"
        >
          {{ logo }}
        </li>
      </ul>
      <ul class="flex items-center gap-12" aria-hidden="true">
        <li
          v-for="logo in logos"
          :key="`dup-${logo}`"
          class="text-lg font-bold tracking-tight text-muted-foreground/70"
        >
          {{ logo }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
/**
 * Marquee（Magic UI marquee 的 CSS 复刻）
 *
 * 两份内容首尾相接 + translateX(-50%) 循环；第二份对读屏隐藏。
 * reduced-motion 下动画停止（motion-reduce:animate-none），静态列表仍可读。
 */
defineProps({
  logos: { type: Array, default: () => [] },
})
</script>
