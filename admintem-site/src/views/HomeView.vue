<template>
  <div>
    <!-- ================= Hero ================= -->
    <section class="relative overflow-hidden pb-16 pt-14 sm:pb-24 sm:pt-20">
      <div class="grid-pattern pointer-events-none absolute inset-0 -z-10" aria-hidden="true" />
      <div
        class="pointer-events-none absolute -right-40 -top-40 -z-10 h-[520px] w-[520px] rounded-full bg-primary/10 blur-3xl"
        aria-hidden="true"
      />
      <div
        class="pointer-events-none absolute -left-40 top-40 -z-10 h-[420px] w-[420px] rounded-full bg-[#7c3aed]/10 blur-3xl"
        aria-hidden="true"
      />

      <div class="container grid items-center gap-14 lg:grid-cols-[1.02fr_1fr]">
        <div>
          <p class="hero-eyebrow eyebrow">
            <Sparkles class="h-3.5 w-3.5 text-primary" aria-hidden="true" />
            {{ site.hero.eyebrow }}
          </p>

          <h1 class="mt-5 text-4xl font-extrabold leading-[1.12] tracking-tight text-foreground sm:text-5xl lg:text-[54px]">
            <span class="hero-title-line block">{{ site.hero.title[0] }}</span>
            <span class="hero-title-line block">
              收进
              <span class="text-gradient">{{ site.hero.highlight }}</span>
            </span>
          </h1>

          <p class="hero-desc mt-6 max-w-xl text-base leading-7 text-muted-foreground sm:text-lg">
            {{ site.hero.description }}
          </p>

          <div class="hero-actions mt-8 flex flex-col gap-3 sm:flex-row">
            <Button :to="site.actions.primary.to" size="lg">
              免费开始
              <ArrowRight class="h-4 w-4" aria-hidden="true" />
            </Button>
            <Button :to="site.actions.secondary.to" variant="secondary" size="lg">
              <PlayCircle class="h-4 w-4" aria-hidden="true" />
              预约演示
            </Button>
          </div>

          <p class="mt-4 text-xs text-muted-foreground">14 天试用 · 无需信用卡 · 随时导出数据</p>
        </div>

        <div class="hero-preview relative">
          <ProductPreview />
          <div
            class="absolute -bottom-5 -left-3 hidden animate-float rounded-lg border border-border bg-card/95 px-4 py-3 shadow-lift backdrop-blur sm:block"
          >
            <p class="text-[11px] text-muted-foreground">本周为团队节省</p>
            <p class="mt-1 text-lg font-bold text-foreground">6.5 <span class="text-xs font-medium text-muted-foreground">小时</span></p>
          </div>
        </div>
      </div>

      <!-- 指标 -->
      <div class="container mt-16">
        <dl class="grid gap-4 rounded-lg border border-border bg-card/70 p-2 shadow-soft backdrop-blur sm:grid-cols-3">
          <div v-for="stat in site.hero.stats" :key="stat.label" class="rounded-md px-5 py-5">
            <dt class="text-sm text-muted-foreground">{{ stat.label }}</dt>
            <dd class="mt-1 text-3xl font-extrabold tracking-tight text-foreground">
              <NumberTicker :value="stat.value" :suffix="stat.suffix" :decimals="stat.decimals || 0" />
            </dd>
          </div>
        </dl>
      </div>
    </section>

    <!-- ================= Logos ================= -->
    <section class="border-y border-border bg-card/40 py-10" aria-label="客户 logo">
      <div class="container">
        <p class="mb-6 text-center text-xs font-medium uppercase tracking-[0.2em] text-muted-foreground">
          12,000+ 团队用它保持同步
        </p>
        <Marquee :logos="site.logos" />
      </div>
    </section>

    <!-- ================= Features（Bento） ================= -->
    <section id="features" class="section">
      <div class="container">
        <SectionHeading
          v-reveal
          eyebrow="能力"
          title="一个空间，覆盖协作的完整链路"
          desc="从写下第一行需求，到复盘最后一次发布，上下文始终在同一个地方。"
          center
        />

        <div class="mt-12 grid gap-4 lg:grid-cols-6">
          <article
            v-for="(feature, index) in site.features"
            :key="feature.title"
            v-reveal="index * 60"
            class="group relative flex h-full flex-col overflow-hidden rounded-lg border border-border bg-card p-6 shadow-soft transition-shadow duration-300 hover:shadow-lift"
            :class="feature.span"
          >
            <BorderBeam v-if="index === 2" />
            <span class="grid h-10 w-10 place-items-center rounded-lg bg-primary/10 text-primary">
              <component :is="icons[feature.icon]" class="h-5 w-5" aria-hidden="true" />
            </span>
            <h3 class="mt-4 text-lg font-bold text-foreground">{{ feature.title }}</h3>
            <p class="mt-2 text-sm leading-7 text-muted-foreground">{{ feature.desc }}</p>

            <div v-if="index === 0" class="mt-auto space-y-2 pt-6" aria-hidden="true">
              <div class="h-2 w-2/3 rounded-full bg-muted" />
              <div class="h-2 w-full rounded-full bg-muted" />
              <div class="h-2 w-4/5 rounded-full bg-muted" />
              <div class="mt-3 flex gap-2">
                <span class="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-medium text-primary">需求</span>
                <span class="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">评审</span>
                <span class="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">已完成</span>
              </div>
            </div>

            <div v-else-if="index === 1" class="mt-auto flex items-end gap-1.5 pt-6" aria-hidden="true">
              <span v-for="h in [38, 52, 44, 68, 60, 82, 74]" :key="h" class="w-full rounded-sm bg-primary/15" :style="{ height: `${h}px` }" />
            </div>
            <div v-else-if="index === 2" class="mt-auto rounded-md border border-border/70 bg-muted/40 p-3 text-xs text-muted-foreground">
              <p class="flex items-center gap-1.5 font-medium text-foreground">
                <Sparkles class="h-3.5 w-3.5 text-primary" aria-hidden="true" />
                这段决策的依据是什么？
              </p>
              <p class="mt-1.5 leading-6">检索到 3 条相关记录，引用自《Q4 发布评审》与 2 篇周报。</p>
            </div>

            <div v-else-if="index === 3" class="mt-auto flex flex-wrap items-center gap-2 pt-6" aria-hidden="true">
              <span class="rounded-md border border-border bg-muted/40 px-2.5 py-1 text-[10px] font-medium text-muted-foreground">触发器</span>
              <span class="text-muted-foreground/50">→</span>
              <span class="rounded-md border border-border bg-muted/40 px-2.5 py-1 text-[10px] font-medium text-muted-foreground">条件</span>
              <span class="text-muted-foreground/50">→</span>
              <span class="rounded-md bg-primary/10 px-2.5 py-1 text-[10px] font-medium text-primary">自动归档</span>
            </div>

            <div v-else-if="index === 4" class="mt-auto space-y-2 pt-6" aria-hidden="true">
              <div v-for="(scope, i) in ['空间', '文档', '段落']" :key="scope" class="flex items-center gap-2 rounded-md border border-border/70 px-2.5 py-1.5">
                <ShieldCheck class="h-3.5 w-3.5" :class="i === 2 ? 'text-primary' : 'text-muted-foreground/60'" />
                <span class="text-[11px] text-muted-foreground">{{ scope }}</span>
                <span class="ml-auto text-[10px] text-muted-foreground/70">{{ i === 2 ? '仅成员' : '团队' }}</span>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- ================= Steps ================= -->
    <section class="section border-y border-border bg-card/40">
      <div class="container">
        <SectionHeading
          v-reveal
          eyebrow="上手"
          title="三步把团队搬进来"
          desc="不需要一次性迁移完。先从一个真实项目开始，验证顺了再铺开。"
          center
        />

        <ol class="mt-12 grid gap-6 md:grid-cols-3">
          <li v-for="(step, index) in site.steps" :key="step.no" v-reveal="index * 80" class="relative rounded-lg border border-border bg-card p-6 shadow-soft">
            <span class="text-sm font-bold text-primary">{{ step.no }}</span>
            <h3 class="mt-3 text-lg font-bold text-foreground">{{ step.title }}</h3>
            <p class="mt-2 text-sm leading-7 text-muted-foreground">{{ step.desc }}</p>
          </li>
        </ol>
      </div>
    </section>

    <!-- ================= Testimonials ================= -->
    <section class="section">
      <div class="container">
        <SectionHeading v-reveal eyebrow="客户" title="他们用 Nova 减少了切换" center />

        <div class="mt-12 grid gap-4 md:grid-cols-3">
          <figure
            v-for="(item, index) in site.testimonials"
            :key="item.name"
            v-reveal="index * 70"
            class="flex h-full flex-col rounded-lg border border-border bg-card p-6 shadow-soft"
          >
            <blockquote class="flex-1 text-sm leading-7 text-foreground/90">“{{ item.quote }}”</blockquote>
            <figcaption class="mt-5 flex items-center gap-3 border-t border-border pt-4">
              <span class="grid h-9 w-9 place-items-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                {{ item.initials }}
              </span>
              <span>
                <span class="block text-sm font-semibold text-foreground">{{ item.name }}</span>
                <span class="block text-xs text-muted-foreground">{{ item.role }}</span>
              </span>
            </figcaption>
          </figure>
        </div>
      </div>
    </section>

    <!-- ================= Pricing preview ================= -->
    <section id="pricing" class="section border-y border-border bg-card/40">
      <div class="container">
        <SectionHeading
          v-reveal
          eyebrow="价格"
          title="按团队规模，选一个开始"
          desc="所有价格均为示例，接入真实支付前替换为你的定价。"
          center
        />

        <div class="mt-12 grid items-stretch gap-4 md:grid-cols-3">
          <PricingCard v-for="plan in site.pricing.plans" :key="plan.name" :plan="plan" />
        </div>

        <div class="mt-8 text-center">
          <RouterLink to="/pricing" class="text-sm font-semibold text-primary hover:underline">
            查看完整价格与功能对比 →
          </RouterLink>
        </div>
      </div>
    </section>

    <!-- ================= FAQ ================= -->
    <section class="section">
      <div class="container grid gap-10 lg:grid-cols-[0.8fr_1.2fr]">
        <div v-reveal>
          <SectionHeading eyebrow="常见问题" title="先把疑问说清楚" desc="没找到答案？直接联系我们，通常一个工作日内回复。" />
          <Button to="/contact" variant="outline" class="mt-6">联系我们</Button>
        </div>
        <div v-reveal="80">
          <Accordion :items="site.faq" />
        </div>
      </div>
    </section>

    <CtaSection />
  </div>
</template>

<script setup>
/**
 * 首页
 *
 * 结构遵循 ui-ux-pro-max 的 Hero + Features + CTA 模式：
 * 首屏（主张 + 产品预览）→ 社会证明 → 能力 → 上手 → 客户 → 价格 → FAQ → CTA。
 * 动效只做两件事：首屏编队入场（anime timeline）与滚动入场（v-reveal）。
 */
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowRight, FileText, ListChecks, PlayCircle, ShieldCheck, Sparkles, Workflow } from 'lucide-vue-next'
import { createTimeline } from 'animejs'
import { site } from '@/content/site'
import Button from '@/components/ui/Button.vue'
import SectionHeading from '@/components/ui/SectionHeading.vue'
import Accordion from '@/components/ui/Accordion.vue'
import NumberTicker from '@/components/magic/NumberTicker.vue'
import Marquee from '@/components/magic/Marquee.vue'
import BorderBeam from '@/components/magic/BorderBeam.vue'
import ProductPreview from '@/components/site/ProductPreview.vue'
import PricingCard from '@/components/site/PricingCard.vue'
import CtaSection from '@/components/site/CtaSection.vue'

const icons = { FileText, ListChecks, Sparkles, Workflow, ShieldCheck }

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

  const tl = createTimeline({ defaults: { ease: 'outExpo', duration: 760 } })
  tl.add('.hero-eyebrow', { opacity: [0, 1], y: [10, 0] })
    .add('.hero-title-line', { opacity: [0, 1], y: [26, 0], delay: 60 }, '-=560')
    .add('.hero-desc', { opacity: [0, 1], y: [14, 0] }, '-=600')
    .add('.hero-actions', { opacity: [0, 1], y: [12, 0] }, '-=620')
    .add('.hero-preview', { opacity: [0, 1], y: [22, 0], scale: [0.985, 1] }, '-=680')
})
</script>
