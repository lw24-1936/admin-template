<template>
  <span ref="root" class="tabular-nums">{{ display }}<span v-if="suffix">{{ suffix }}</span></span>
</template>

<script setup>
/**
 * NumberTicker（Magic UI number-ticker 的 Vue/anime.js 复刻）
 *
 * 进入视口后从 0 数到目标值；reduced-motion 直接显示最终值。
 * 只改文本，不动布局属性，避免 CLS。
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { animate } from 'animejs'

const props = defineProps({
  value: { type: Number, required: true },
  suffix: { type: String, default: '' },
  decimals: { type: Number, default: 0 },
  duration: { type: Number, default: 1400 },
})

const root = ref(null)
const current = ref(0)
let observer = null
let animation = null

const formatter = computed(
  () =>
    new Intl.NumberFormat('zh-CN', {
      minimumFractionDigits: props.decimals,
      maximumFractionDigits: props.decimals,
    })
)

const display = computed(() => formatter.value.format(current.value))

function run() {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    current.value = props.value
    return
  }
  const counter = { v: 0 }
  animation = animate(counter, {
    v: props.value,
    duration: props.duration,
    ease: 'outExpo',
    onUpdate: () => {
      current.value = counter.v
    },
  })
}

onMounted(() => {
  if (!root.value) return
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        observer.disconnect()
        run()
      }
    },
    { threshold: 0.4 }
  )
  observer.observe(root.value)
})

onUnmounted(() => {
  observer?.disconnect()
  animation?.pause?.()
})
</script>
