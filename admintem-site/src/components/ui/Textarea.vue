<template>
  <textarea
    :value="modelValue"
    :id="id"
    :name="name"
    :rows="rows"
    :placeholder="placeholder"
    :aria-invalid="invalid || undefined"
    :aria-describedby="describedby"
    :class="
      cn(
        'w-full rounded-lg border border-border bg-card px-3.5 py-3 text-sm leading-6 text-foreground shadow-sm transition-colors placeholder:text-muted-foreground/70 focus:border-primary focus:outline-none',
        invalid && 'border-red-400 focus:border-red-500',
        $props.class
      )
    "
    @input="$emit('update:modelValue', $event.target.value)"
  />
</template>

<script setup>
/** Textarea：与 Input 同一套边框/焦点/错误态 */
import { cn } from '@/lib/utils'

defineProps({
  modelValue: { type: String, default: '' },
  id: { type: String, default: undefined },
  name: { type: String, default: undefined },
  rows: { type: Number, default: 5 },
  placeholder: { type: String, default: '' },
  invalid: { type: Boolean, default: false },
  describedby: { type: String, default: undefined },
  class: { type: String, default: '' },
})
defineEmits(['update:modelValue'])
</script>
