<template>
  <div>
    <section class="relative overflow-hidden border-b border-border pb-14 pt-16">
      <div class="grid-pattern pointer-events-none absolute inset-0 -z-10" aria-hidden="true" />
      <div class="container max-w-3xl text-center">
        <p class="eyebrow">价格</p>
        <h1 class="mt-4 text-4xl font-extrabold tracking-tight text-foreground sm:text-5xl balance">
          按团队规模，选一个开始
        </h1>
        <p class="mt-5 text-base leading-7 text-muted-foreground">
          所有价格均为示例；接入真实支付前，替换 content/site.js 中的定价与 CTA 链接即可。
        </p>

        <!-- 计费周期切换：button + aria-pressed，避免用 checkbox 伪装 -->
        <div class="mt-8 inline-flex items-center rounded-full border border-border bg-card p-1 shadow-soft" role="group" aria-label="计费周期">
          <button
            v-for="option in options"
            :key="option.value"
            type="button"
            class="rounded-full px-4 py-1.5 text-sm font-semibold transition-colors"
            :class="yearly === option.value ? 'bg-primary text-primary-foreground shadow-soft' : 'text-muted-foreground hover:text-foreground'"
            :aria-pressed="yearly === option.value"
            @click="yearly = option.value"
          >
            {{ option.label }}
            <span v-if="option.value" class="ml-1 text-[11px] font-medium opacity-80">省 20%</span>
          </button>
        </div>
      </div>
    </section>

    <section class="section pt-12">
      <div class="container">
        <div class="grid items-stretch gap-4 md:grid-cols-3">
          <PricingCard v-for="plan in site.pricing.plans" :key="plan.name" :plan="plan" :yearly="yearly" />
        </div>
        <p class="mt-6 text-center text-xs text-muted-foreground">{{ site.pricing.note }}</p>
      </div>
    </section>

    <section class="section border-y border-border bg-card/40">
      <div class="container">
        <SectionHeading eyebrow="对比" title="三档方案的差异" center />
        <div class="mx-auto mt-10 max-w-4xl overflow-hidden rounded-lg border border-border bg-card shadow-soft">
          <table class="w-full text-left text-sm">
            <caption class="sr-only">方案功能对比</caption>
            <thead class="bg-muted/50 text-xs uppercase tracking-wide text-muted-foreground">
              <tr>
                <th scope="col" class="px-4 py-3 font-semibold">能力</th>
                <th v-for="plan in site.pricing.plans" :key="plan.name" scope="col" class="px-4 py-3 font-semibold">
                  {{ plan.name }}
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-border">
              <tr v-for="row in matrix" :key="row.label">
                <th scope="row" class="px-4 py-3 font-medium text-foreground">{{ row.label }}</th>
                <td v-for="(value, i) in row.values" :key="i" class="px-4 py-3 text-muted-foreground">
                  <Check v-if="value === true" class="h-4 w-4 text-primary" aria-label="支持" />
                  <span v-else-if="value === false" aria-label="不支持">—</span>
                  <template v-else>{{ value }}</template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="container grid gap-10 lg:grid-cols-[0.8fr_1.2fr]">
        <SectionHeading eyebrow="常见问题" title="关于价格与迁移" />
        <Accordion :items="site.faq" />
      </div>
    </section>

    <CtaSection />
  </div>
</template>

<script setup>
/** 价格页：周期切换 + 三档卡片 + 对比表 + FAQ */
import { ref } from 'vue'
import { Check } from 'lucide-vue-next'
import { site } from '@/content/site'
import PricingCard from '@/components/site/PricingCard.vue'
import SectionHeading from '@/components/ui/SectionHeading.vue'
import Accordion from '@/components/ui/Accordion.vue'
import CtaSection from '@/components/site/CtaSection.vue'

const yearly = ref(false)
const options = [
  { label: '月付', value: false },
  { label: '年付', value: true },
]

const matrix = [
  { label: '成员上限', values: ['5 人', '不限', '不限'] },
  { label: '文档与任务', values: ['10 个空间', '无限', '无限'] },
  { label: 'AI 摘要与检索', values: [false, true, true] },
  { label: '无代码自动化', values: [false, true, true] },
  { label: 'SSO / SCIM', values: [false, false, true] },
  { label: '审计日志', values: [false, false, true] },
  { label: '支持', values: ['社区', '优先', '专属客户成功'] },
]
</script>
