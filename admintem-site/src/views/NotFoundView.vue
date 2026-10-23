<template>
  <section class="section">
    <div class="container max-w-xl text-center">
      <p class="text-6xl font-extrabold tracking-tight text-primary">404</p>
      <h1 class="mt-4 text-3xl font-bold tracking-tight text-foreground">页面走丢了</h1>
      <p class="mt-3 text-base leading-7 text-muted-foreground">
        链接可能已经过期，或者地址拼写有误。回到首页继续浏览，或者直接联系我们。
      </p>
      <div class="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
        <Button to="/" size="lg">回到首页</Button>
        <Button to="/contact" variant="secondary" size="lg">联系我们</Button>
      </div>
    </div>
  </section>
</template>

<script setup>
/** 404：给两条明确出路，避免死胡同 */
import Button from '@/components/ui/Button.vue'
</script>
