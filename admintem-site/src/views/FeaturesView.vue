<template>
  <div>
    <section class="relative overflow-hidden border-b border-border pb-16 pt-16">
      <div class="grid-pattern pointer-events-none absolute inset-0 -z-10" aria-hidden="true" />
      <div class="container max-w-3xl text-center">
        <p class="eyebrow">功能</p>
        <h1 class="mt-4 text-4xl font-extrabold tracking-tight text-foreground sm:text-5xl balance">
          把散落的信息，变成可复用的上下文
        </h1>
        <p class="mt-5 text-base leading-7 text-muted-foreground sm:text-lg">
          每项能力都围绕同一个目标：让团队在需要的时候，能找到、能引用、能继续往下做。
        </p>
        <div class="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
          <Button to="/pricing" size="lg">免费开始</Button>
          <Button to="/contact" variant="secondary" size="lg">预约演示</Button>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="container space-y-20">
        <article
          v-for="(feature, index) in site.features"
          :key="feature.title"
          v-reveal
          class="grid items-center gap-10 lg:grid-cols-2"
        >
          <div :class="index % 2 === 1 ? 'lg:order-2' : ''">
            <span class="grid h-11 w-11 place-items-center rounded-lg bg-primary/10 text-primary">
              <component :is="icons[feature.icon]" class="h-5 w-5" aria-hidden="true" />
            </span>
            <h2 class="mt-4 text-2xl font-bold tracking-tight text-foreground">{{ feature.title }}</h2>
            <p class="mt-3 text-base leading-7 text-muted-foreground">{{ feature.desc }}</p>
            <ul class="mt-5 space-y-2 text-sm text-muted-foreground">
              <li v-for="point in points[index]" :key="point" class="flex items-start gap-2">
                <Check class="mt-0.5 h-4 w-4 shrink-0 text-primary" aria-hidden="true" />
                {{ point }}
              </li>
            </ul>
          </div>

          <div
            class="relative overflow-hidden rounded-lg border border-border bg-card p-6 shadow-soft"
            :class="index % 2 === 1 ? 'lg:order-1' : ''"
            aria-hidden="true"
          >
            <div class="space-y-2">
              <div class="h-2.5 w-24 rounded-full bg-primary/20" />
              <div class="h-2 w-full rounded-full bg-muted" />
              <div class="h-2 w-5/6 rounded-full bg-muted" />
              <div class="mt-4 grid grid-cols-3 gap-2">
                <div v-for="n in 3" :key="n" class="rounded-md border border-border bg-muted/40 p-3">
                  <div class="h-1.5 w-8 rounded-full bg-primary/30" />
                  <div class="mt-2 h-4 w-10 rounded bg-foreground/10" />
                </div>
              </div>
              <div class="mt-4 space-y-1.5">
                <div v-for="n in 3" :key="n" class="flex items-center gap-2 rounded-md border border-border/70 px-2.5 py-2">
                  <span class="h-3 w-3 rounded-full border border-primary/40" />
                  <span class="h-1.5 flex-1 rounded-full bg-muted" />
                </div>
              </div>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section class="section border-y border-border bg-card/40">
      <div class="container">
        <SectionHeading eyebrow="集成" title="和你已经在用的工具配合" desc="通过 Webhook、API 与常见办公工具连接，不需要替换现有栈。" center />
        <div class="mx-auto mt-10 grid max-w-4xl grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
          <div
            v-for="tool in tools"
            :key="tool"
            v-reveal
            class="rounded-lg border border-border bg-card px-4 py-5 text-center text-sm font-semibold text-muted-foreground shadow-soft"
          >
            {{ tool }}
          </div>
        </div>
      </div>
    </section>

    <CtaSection />
  </div>
</template>

<script setup>
/** 功能页：首屏主张 + 交替图文（每项能力一段）+ 集成 + CTA */
import { FileText, ListChecks, ShieldCheck, Sparkles, Workflow, Check } from 'lucide-vue-next'
import { site } from '@/content/site'
import Button from '@/components/ui/Button.vue'
import SectionHeading from '@/components/ui/SectionHeading.vue'
import CtaSection from '@/components/site/CtaSection.vue'

const icons = { FileText, ListChecks, Sparkles, Workflow, ShieldCheck }

const points = [
  ['文档与任务共享同一上下文', '评论、修订与负责人自动关联', '支持 Markdown / 富文本混排'],
  ['待办一键转为任务', '状态变化自动同步看板', '周报自动汇总进展'],
  ['回答附带引用来源', '只检索有权限的内容', '支持关闭 AI 功能'],
  ['触发器 + 条件 + 动作', '审批 / 提醒 / 归档模板', '运行记录可追溯'],
  ['空间 / 文档 / 段落三级权限', '外部协作者可见范围可控', '审计日志记录访问'],
]

const tools = ['飞书', '企业微信', 'Slack', 'GitHub', 'Figma', 'Zapier']
</script>
