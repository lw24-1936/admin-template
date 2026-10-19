<template>
  <!--
    状态页（403 / 404 共用）：纸面画布 + 编辑式排版
    - 大号数字作为视觉锚点，kicker 提供英文标签，标题与说明交代"为什么"和"怎么办"
    - 与登录页共用同一底色与网格，保持全站视觉一致
  -->
  <main class="status-page">
    <section class="status-sheet">
      <p class="status-kicker">{{ kicker }}</p>
      <p class="status-code">{{ code }}</p>
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
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--lc-sp-8) var(--lc-sp-6);
  background:
    linear-gradient(var(--lc-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--lc-grid-line) 1px, transparent 1px),
    var(--lc-paper);
  background-size: 32px 32px;
}
.status-sheet {
  width: 100%;
  max-width: 520px;
  padding: var(--lc-sp-7) var(--lc-sp-7) var(--lc-sp-6);
  background: var(--lc-paper-card);
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-card);
  box-shadow: var(--lc-shadow-sheet);
  animation: status-in var(--lc-motion-enter) both;
}
.status-kicker {
  margin: 0 0 var(--lc-sp-3);
  font-size: var(--lc-fs-kicker);
  letter-spacing: 2.6px;
  text-transform: uppercase;
  color: var(--lc-ink-faint);
}
.status-code {
  margin: 0;
  font-size: 72px;
  line-height: 1;
  font-weight: 650;
  letter-spacing: 2px;
  color: var(--lc-brand);
  font-variant-numeric: tabular-nums;
}
.status-title {
  margin: var(--lc-sp-4) 0 var(--lc-sp-3);
  font-size: var(--lc-fs-page);
  font-weight: 650;
  letter-spacing: 0.4px;
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
  gap: var(--lc-sp-3);
  margin-top: var(--lc-sp-5);
}
@keyframes status-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
