<template>
  <div
    :class="
      cn(
        'rounded-lg border border-border bg-card text-card-foreground shadow-soft',
        glass && 'glass',
        padded && 'p-6',
        hover && 'transition-shadow duration-300 hover:shadow-lift',
        $props.class
      )
    "
  >
    <slot />
  </div>
</template>

<script setup>
/** Card：白卡 / 玻璃卡两种形态，圆角与阴影来自 token */
import { cn } from '@/lib/utils'

defineProps({
  glass: { type: Boolean, default: false },
  padded: { type: Boolean, default: false },
  hover: { type: Boolean, default: false },
  class: { type: String, default: '' },
})
</script>
