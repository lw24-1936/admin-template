import { animate } from 'animejs'

const reduced = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches

/**
 * v-reveal：滚动进入视口时做一次入场（opacity + y），只跑一次。
 *
 * 设计取舍：
 *   1. 初始隐藏由 JS 添加 `.reveal-init`（CSS 不写死 opacity:0），JS 失败时内容仍可见；
 *   2. 用 IntersectionObserver 控制触发，避免离屏动画空转；
 *   3. reduced-motion 下直接跳过，元素保持最终态。
 *
 * 用法：<section v-reveal> 或 <div v-reveal="120">（延迟 ms）
 */
export default {
  mounted(el, binding) {
    if (reduced()) return

    const delay = typeof binding.value === 'number' ? binding.value : 0
    el.classList.add('reveal-init')

    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          io.unobserve(el)
          el.classList.remove('reveal-init')
          animate(el, {
            opacity: [0, 1],
            y: [18, 0],
            duration: 700,
            delay,
            ease: 'outCubic',
          })
        })
      },
      { rootMargin: '0px 0px -12% 0px', threshold: 0.08 }
    )

    io.observe(el)
    el._revealObserver = io
  },
  unmounted(el) {
    el._revealObserver?.disconnect()
  },
}
