<template>
  <div>
    <section class="relative overflow-hidden border-b border-border pb-16 pt-16">
      <div class="grid-pattern pointer-events-none absolute inset-0 -z-10" aria-hidden="true" />
      <div class="container grid items-center gap-10 lg:grid-cols-[1.1fr_0.9fr]">
        <div>
          <p class="eyebrow">关于</p>
          <h1 class="mt-4 text-4xl font-extrabold tracking-tight text-foreground sm:text-5xl balance">
            {{ site.about.title }}
          </h1>
          <p class="mt-5 max-w-xl text-base leading-7 text-muted-foreground">
            {{ site.about.desc }}
          </p>
        </div>
        <dl class="grid grid-cols-2 gap-3">
          <div v-for="stat in aboutStats" :key="stat.label" class="rounded-lg border border-border bg-card p-5 shadow-soft">
            <dt class="text-xs text-muted-foreground">{{ stat.label }}</dt>
            <dd class="mt-1 text-2xl font-extrabold tracking-tight text-foreground">{{ stat.value }}</dd>
          </div>
        </dl>
      </div>
    </section>

    <section class="section">
      <div class="container">
        <SectionHeading eyebrow="原则" title="我们怎么做产品" center />
        <div class="mt-12 grid gap-4 md:grid-cols-3">
          <article
            v-for="(value, index) in site.about.values"
            :key="value.title"
            v-reveal="index * 70"
            class="rounded-lg border border-border bg-card p-6 shadow-soft"
          >
            <span class="text-sm font-bold text-primary">0{{ index + 1 }}</span>
            <h2 class="mt-3 text-lg font-bold text-foreground">{{ value.title }}</h2>
            <p class="mt-2 text-sm leading-7 text-muted-foreground">{{ value.desc }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="section border-y border-border bg-card/40">
      <div class="container grid gap-10 lg:grid-cols-[0.8fr_1.2fr]">
        <SectionHeading eyebrow="历程" title="一步一步走到现在" desc="不追热点，只把协作这件事做扎实。" />
        <ol class="relative space-y-8 border-l border-border pl-6">
          <li v-for="item in site.about.milestones" :key="item.year" v-reveal class="relative">
            <span class="absolute -left-[31px] top-1.5 grid h-3 w-3 place-items-center rounded-full border-2 border-primary bg-background" aria-hidden="true" />
            <p class="text-sm font-bold text-primary">{{ item.year }}</p>
            <p class="mt-1 text-base text-foreground">{{ item.text }}</p>
          </li>
        </ol>
      </div>
    </section>

    <CtaSection />
  </div>
</template>

<script setup>
/** 关于页：主张 + 原则 + 时间线；数字均为模版示例 */
import { site } from '@/content/site'
import SectionHeading from '@/components/ui/SectionHeading.vue'
import CtaSection from '@/components/site/CtaSection.vue'

const aboutStats = [
  { label: '团队规模', value: '38 人' },
  { label: '服务团队', value: '12,000+' },
  { label: '成立时间', value: '2023' },
  { label: '远程优先', value: '6 城市' },
]
</script>
