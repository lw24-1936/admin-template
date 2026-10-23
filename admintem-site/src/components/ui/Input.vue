<template>
  <input
    :value="modelValue"
    :type="type"
    :id="id"
    :name="name"
    :placeholder="placeholder"
    :autocomplete="autocomplete"
    :aria-invalid="invalid || undefined"
    :aria-describedby="describedby"
    :class="
      cn(
        'h-11 w-full rounded-lg border border-border bg-card px-3.5 text-sm text-foreground shadow-sm transition-colors placeholder:text-muted-foreground/70 focus:border-primary focus:outline-none',
        invalid && 'border-red-400 focus:border-red-500',
        $props.class
      )
    "
    @input="$emit('update:modelValue', $event.target.value)"
  />
</template>

<script setup>
/** Input：受控输入，错误态通过 aria-invalid + 边框同时表达（不只靠颜色） */
import { cn } from '@/lib/utils'

defineProps({
  modelValue: { type: String, default: '' },
  type: { type: String, default: 'text' },
  id: { type: String, default: undefined },
  name: { type: String, default: undefined },
  placeholder: { type: String, default: '' },
  autocomplete: { type: String, default: 'off' },
  invalid: { type: Boolean, default: false },
  describedby: { type: String, default: undefined },
  class: { type: String, default: '' },
})
defineEmits(['update:modelValue'])
</script>
