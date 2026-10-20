<template>
  <!--
    状态页（403 / 404 共用）：渐变画布 + 一张"告示纸"
    - 大号状态码是视觉锚点（主色渐变文字），kicker 给英文标签
    - 标题与说明交代"为什么被拦 / 怎么办"，操作区由使用方传入
  -->
  <main class="status-page">
    <section class="status-sheet">
      <div class="status-head">
        <p class="status-kicker">{{ kicker }}</p>
        <p class="status-code">{{ code }}</p>
      </div>
      <h1 class="status-title">{{ title }}</h1>
      <p class="status-desc">{{ desc }}</p>
      <div class="status-actions">
        <slot />
      </div>
    </section>
  </main>
</template>

<script setup>
/**
 * 公共状态页组件（错误页/空态页）
 *
 * 用法：
 *   <StatusPage code="403" kicker="ACCESS DENIED" title="没有访问权限" desc="...">
 *     <el-button type="primary" @click="router.replace('/')">返回首页</el-button>
 *   </StatusPage>
 *
 * 视觉：与登录页同一套"渐变画布 + 网格 + 光斑"，但只保留一张告示纸，
 * 不叠加动效组件 —— 错误页首先要把原因读清楚。
 */
defineProps({
  /** 大号状态码（如 403 / 404） */
  code: { type: String, required: true },
  /** 全大写英文标签（编辑式排版用） */
  kicker: { type: String, default: '' },
  /** 中文标题 */
  title: { type: String, required: true },
  /** 一行说明：原因 + 处理方式 */
  desc: { type: String, default: '' },
})
</script>

<style scoped>
.status-page {
  position: relative;
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--lc-sp-8) var(--lc-sp-6);
  overflow: hidden;
  background-image:
    radial-gradient(760px 380px at 12% -10%, rgba(255, 255, 255, 0.95), transparent 62%),
    radial-gradient(620px 340px at 100% 6%, var(--lc-accent-soft), transparent 64%),
    var(--lc-gradient-hero);
  background-color: var(--lc-paper);
}
/* 网格与右下光斑：纯装饰层，不吃事件 */
.status-page::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(var(--lc-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--lc-grid-line) 1px, transparent 1px);
  background-size: 30px 30px;
  mask-image: radial-gradient(120% 90% at 50% 0%, rgba(0, 0, 0, 0.85), transparent 78%);
}
.status-page::after {
  content: '';
  position: absolute;
  width: 380px;
  height: 380px;
  right: -120px;
  bottom: -200px;
  pointer-events: none;
  border-radius: 50%;
  background: radial-gradient(circle, var(--lc-brand-glow) 0%, transparent 62%);
}

.status-sheet {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 520px;
  padding: var(--lc-sp-7) var(--lc-sp-7) var(--lc-sp-6);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(14px) saturate(1.1);
  box-shadow: var(--lc-shadow-float);
  animation: status-in var(--lc-motion-enter) var(--lc-ease-out) both;
}

.status-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--lc-sp-4);
}
.status-kicker {
  margin: 6px 0 0;
  font-size: var(--lc-fs-kicker);
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--lc-ink-faint);
}
.status-code {
  margin: 0;
  font-size: 76px;
  line-height: 0.9;
  font-weight: 700;
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
  /* 渐变文字：两端都在深色区间，最深色仍达 9:1，不牺牲可读性 */
  background-image: var(--lc-gradient-brand);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.status-title {
  margin: var(--lc-sp-5) 0 var(--lc-sp-3);
  font-size: var(--lc-fs-page);
  font-weight: 680;
  letter-spacing: -0.02em;
  color: var(--lc-ink);
}
.status-desc {
  margin: 0;
  padding-top: var(--lc-sp-3);
  border-top: 1px solid var(--lc-line-soft);
  font-size: var(--lc-fs-label);
  line-height: 1.85;
  color: var(--lc-ink-muted);
}
.status-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-sp-3);
  margin-top: var(--lc-sp-5);
}

@keyframes status-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 640px) {
  .status-page {
    padding: var(--lc-sp-6) var(--lc-sp-4);
  }
  .status-sheet {
    padding: var(--lc-sp-6) var(--lc-sp-5) var(--lc-sp-5);
    border-radius: var(--lc-radius-card);
  }
  .status-code {
    font-size: 60px;
  }
}
</style>
