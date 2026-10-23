<template>
  <div :class="cn('max-w-2xl', center && 'mx-auto text-center', $props.class)">
    <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
    <h2 :id="id" class="mt-4 text-3xl font-bold tracking-tight text-foreground sm:text-4xl balance">
      <slot name="title">{{ title }}</slot>
    </h2>
    <p v-if="desc || $slots.desc" class="mt-4 text-base leading-7 text-muted-foreground">
      <slot name="desc">{{ desc }}</slot>
    </p>
  </div>
</template>

<script setup>
/** SectionHeading：统一的区块标题（eyebrow / h2 / 描述） */
import { cn } from '@/lib/utils'

defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, default: '' },
  desc: { type: String, default: '' },
  id: { type: String, default: undefined },
  center: { type: Boolean, default: false },
  class: { type: String, default: '' },
})
</script>
