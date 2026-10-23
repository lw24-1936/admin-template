<template>
  <section class="section">
    <div class="container">
      <div class="relative overflow-hidden rounded-lg border border-primary/20 bg-primary px-6 py-14 text-center shadow-lift sm:px-12">
        <DotPattern class="!-z-0 opacity-20" />
        <div class="relative mx-auto max-w-2xl">
          <h2 class="text-3xl font-bold tracking-tight text-primary-foreground sm:text-4xl balance">
            {{ site.cta.title }}
          </h2>
          <p class="mt-4 text-base leading-7 text-primary-foreground/80">
            {{ site.cta.desc }}
          </p>
          <div class="mt-8 flex flex-col items-center justify-center gap-3 sm:flex-row">
            <Button :to="site.cta.primary.to" variant="secondary" size="lg" class="!text-foreground">
              {{ site.cta.primary.label }}
              <ArrowRight class="h-4 w-4" aria-hidden="true" />
            </Button>
            <Button
              :to="site.cta.secondary.to"
              variant="ghost"
              size="lg"
              class="!text-primary-foreground hover:!bg-white/10"
            >
              {{ site.cta.secondary.label }}
            </Button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
/** 底部 CTA：唯一使用主色实底的大区块，形成页面收束 */
import { ArrowRight } from 'lucide-vue-next'
import { site } from '@/content/site'
import Button from '@/components/ui/Button.vue'
import DotPattern from '@/components/magic/DotPattern.vue'
</script>
