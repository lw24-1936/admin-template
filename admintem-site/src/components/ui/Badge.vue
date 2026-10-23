<template>
  <span :class="cn('inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium', variants[variant], $props.class)">
    <slot />
  </span>
</template>

<script setup>
/** Badge：状态/标签，语义色只来自 token */
import { cn } from '@/lib/utils'

defineProps({
  variant: { type: String, default: 'default' },
  class: { type: String, default: '' },
})

const variants = {
  default: 'bg-primary/10 text-primary',
  secondary: 'bg-muted text-muted-foreground',
  accent: 'bg-accent/10 text-accent',
  outline: 'border border-border text-muted-foreground',
  success: 'bg-emerald-500/10 text-emerald-700',
}
</script>
