<template>
  <div class="divide-y divide-border overflow-hidden rounded-lg border border-border bg-card">
    <div v-for="(item, index) in items" :key="item.q">
      <h3>
        <button
          :id="`${uid}-btn-${index}`"
          type="button"
          class="flex w-full items-center justify-between gap-4 px-5 py-4 text-left text-[15px] font-semibold text-foreground transition-colors hover:bg-muted/50"
          :aria-expanded="openIndex === index"
          :aria-controls="`${uid}-panel-${index}`"
          @click="toggle(index)"
        >
          <span>{{ item.q }}</span>
          <ChevronDown
            class="h-4 w-4 shrink-0 text-muted-foreground transition-transform duration-300"
            :class="openIndex === index ? 'rotate-180' : ''"
            aria-hidden="true"
          />
        </button>
      </h3>
      <div
        :id="`${uid}-panel-${index}`"
        role="region"
        :aria-labelledby="`${uid}-btn-${index}`"
        class="grid transition-[grid-template-rows] duration-300 ease-out"
        :class="openIndex === index ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'"
      >
        <div class="overflow-hidden">
          <p class="px-5 pb-4 text-sm leading-7 text-muted-foreground">{{ item.a }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Accordion（FAQ）
 *
 * 无障碍契约照 Radix/shadcn：button + aria-expanded + aria-controls，
 * 内容用 region 关联标题；高度用 grid-template-rows 过渡，不测 DOM 高度。
 */
import { ref, useId } from 'vue'
import { ChevronDown } from 'lucide-vue-next'

const props = defineProps({
  items: { type: Array, default: () => [] },
})

const uid = useId()
const openIndex = ref(-1)

function toggle(index) {
  openIndex.value = openIndex.value === index ? -1 : index
}
</script>
