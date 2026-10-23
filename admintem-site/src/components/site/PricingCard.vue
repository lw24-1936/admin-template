<template>
  <div
    class="relative flex h-full flex-col rounded-lg border bg-card p-6 shadow-soft"
    :class="plan.highlight ? 'border-primary/40 ring-1 ring-primary/20' : 'border-border'"
  >
    <span
      v-if="plan.highlight"
      class="absolute -top-3 left-6 rounded-full bg-primary px-3 py-1 text-[11px] font-semibold text-primary-foreground shadow-soft"
    >
      最受欢迎
    </span>

    <div class="flex items-baseline justify-between gap-3">
      <h3 class="text-lg font-bold text-foreground">{{ plan.name }}</h3>
      <Badge v-if="plan.highlight" variant="default">推荐</Badge>
    </div>
    <p class="mt-1 text-sm text-muted-foreground">{{ plan.desc }}</p>

    <div class="mt-5 flex items-end gap-1.5">
      <template v-if="price === null">
        <span class="text-3xl font-extrabold tracking-tight text-foreground">定制</span>
      </template>
      <template v-else>
        <span class="text-lg font-semibold text-muted-foreground">¥</span>
        <span class="text-4xl font-extrabold tracking-tight text-foreground">{{ price }}</span>
        <span class="pb-1 text-sm text-muted-foreground">/人/月</span>
      </template>
    </div>
    <p v-if="price !== null && yearly" class="mt-1 text-xs text-muted-foreground">按年付费，已含 8 折</p>
    <p v-else-if="price === 0" class="mt-1 text-xs text-muted-foreground">永久免费，无需信用卡</p>
    <p v-else class="mt-1 text-xs text-muted-foreground">&nbsp;</p>

    <Button
      class="mt-5"
      :variant="plan.highlight ? 'primary' : 'outline'"
      to="/contact"
      block
    >
      {{ plan.cta }}
    </Button>

    <ul class="mt-6 space-y-2.5 text-sm">
      <li v-for="feature in plan.features" :key="feature" class="flex items-start gap-2">
        <Check class="mt-0.5 h-4 w-4 shrink-0 text-primary" aria-hidden="true" />
        <span class="text-muted-foreground">{{ feature }}</span>
      </li>
    </ul>
  </div>
</template>

<script setup>
/** 价格卡：三档共用；highlight 控制强调样式 */
import { computed } from 'vue'
import { Check } from 'lucide-vue-next'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'

const props = defineProps({
  plan: { type: Object, required: true },
  yearly: { type: Boolean, default: false },
})

const price = computed(() => {
  if (props.plan.price === null) return null
  return props.yearly ? props.plan.yearlyPrice : props.plan.price
})
</script>
