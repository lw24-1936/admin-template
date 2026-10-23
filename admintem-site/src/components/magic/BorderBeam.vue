<template>
  <span class="beam" aria-hidden="true">
    <span class="beam__spin" />
  </span>
</template>

<script setup>
/**
 * BorderBeam（Magic UI border-beam 的 CSS 复刻）
 *
 * 旋转的 conic-gradient 通过 mask 只保留 1px 边框，纯 CSS，不占主线程。
 * 装饰元素：aria-hidden，且不参与布局（absolute inset-0）。
 */
</script>

<style scoped>
.beam {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  overflow: hidden;
  mask:
    linear-gradient(#000 0 0) content-box,
    linear-gradient(#000 0 0);
  mask-composite: exclude;
  -webkit-mask:
    linear-gradient(#000 0 0) content-box,
    linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  padding: 1px;
}

.beam__spin {
  position: absolute;
  inset: -60%;
  background: conic-gradient(
    from 0deg,
    transparent 0deg 250deg,
    #2563eb 300deg,
    #7c3aed 330deg,
    transparent 360deg
  );
  animation: beam-rotate 6s linear infinite;
}

@keyframes beam-rotate {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .beam__spin {
    animation: none;
    background: conic-gradient(from 0deg, transparent 0deg 300deg, #2563eb 330deg, transparent 360deg);
  }
}
</style>
