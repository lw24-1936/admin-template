<template>
  <component
    :is="tag"
    :to="to"
    :href="href"
    :type="tag === 'button' ? type : undefined"
    :class="classes"
  >
    <slot name="icon-left" />
    <slot />
    <slot name="icon-right" />
  </component>
</template>

<script setup>
/**
 * Button（shadcn 形态的 Vue 版本）
 *
 * 变体用静态类名映射，保证 Tailwind 能扫描到；不引入 cva 运行时。
 * `to` 渲染 RouterLink，`href` 渲染 a，其余渲染 button。
 */
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { cn } from '@/lib/utils'

const props = defineProps({
  variant: { type: String, default: 'primary' },
  size: { type: String, default: 'md' },
  to: { type: [String, Object], default: null },
  href: { type: String, default: null },
  type: { type: String, default: 'button' },
  block: { type: Boolean, default: false },
  class: { type: String, default: '' },
})

const variants = {
  primary:
    'bg-primary text-primary-foreground shadow-soft hover:bg-primary/90 active:translate-y-px',
  secondary: 'glass text-foreground hover:bg-white active:translate-y-px',
  outline: 'border border-border bg-transparent text-foreground hover:bg-muted/70',
  ghost: 'text-muted-foreground hover:bg-muted/70 hover:text-foreground',
  accent: 'bg-accent text-accent-foreground shadow-soft hover:bg-accent/90 active:translate-y-px',
}

const sizes = {
  sm: 'h-9 px-4 text-sm',
  md: 'h-11 px-5 text-sm',
  lg: 'h-12 px-6 text-[15px]',
}

const tag = computed(() => (props.to ? RouterLink : props.href ? 'a' : 'button'))

const classes = computed(() =>
  cn(
    'inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition-colors duration-200 disabled:pointer-events-none disabled:opacity-50',
    variants[props.variant] || variants.primary,
    sizes[props.size] || sizes.md,
    props.block && 'w-full',
    props.class
  )
)
</script>
