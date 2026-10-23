<template>
  <div>
    <section class="relative overflow-hidden border-b border-border pb-14 pt-16">
      <div class="grid-pattern pointer-events-none absolute inset-0 -z-10" aria-hidden="true" />
      <div class="container max-w-3xl text-center">
        <p class="eyebrow">联系</p>
        <h1 class="mt-4 text-4xl font-extrabold tracking-tight text-foreground sm:text-5xl balance">
          聊聊你的团队怎么协作
        </h1>
        <p class="mt-5 text-base leading-7 text-muted-foreground">
          留下需求或问题，我们通常在一个工作日内回复。也可以直接发邮件。
        </p>
      </div>
    </section>

    <section class="section">
      <div class="container grid gap-8 lg:grid-cols-[0.85fr_1.15fr]">
        <!-- 联系方式 -->
        <div class="space-y-3">
          <a
            v-for="item in contacts"
            :key="item.label"
            :href="item.href || undefined"
            class="flex items-start gap-3 rounded-lg border border-border bg-card p-5 shadow-soft transition-shadow hover:shadow-lift"
            :class="item.href ? '' : 'pointer-events-none'"
          >
            <span class="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-primary/10 text-primary">
              <component :is="item.icon" class="h-4 w-4" aria-hidden="true" />
            </span>
            <span>
              <span class="block text-xs text-muted-foreground">{{ item.label }}</span>
              <span class="mt-1 block text-sm font-semibold text-foreground">{{ item.value }}</span>
            </span>
          </a>

          <div class="rounded-lg border border-dashed border-border p-5 text-xs leading-6 text-muted-foreground">
            表单为前端演示：提交只做本地校验与成功提示，不会发送邮件。接入后端时替换
            <code class="rounded bg-muted px-1 py-0.5">submit()</code> 即可。
          </div>
        </div>

        <!-- 表单 -->
        <div class="rounded-lg border border-border bg-card p-6 shadow-soft sm:p-8">
          <div v-if="submitted" role="status" class="rounded-lg border border-emerald-200 bg-emerald-50 p-6">
            <h2 class="text-lg font-bold text-emerald-900">已收到，谢谢！</h2>
            <p class="mt-2 text-sm leading-7 text-emerald-800">
              我们会在一个工作日内通过邮件联系 {{ form.email || '你' }}。
            </p>
            <Button class="mt-5" variant="outline" @click="reset">再填一份</Button>
          </div>

          <form v-else novalidate @submit.prevent="submit">
            <h2 class="text-lg font-bold text-foreground">发送需求</h2>
            <p class="mt-1 text-sm text-muted-foreground">带 * 为必填项。</p>

            <div class="mt-6 grid gap-5 sm:grid-cols-2">
              <div>
                <label for="name" class="text-sm font-medium text-foreground">称呼 *</label>
                <Input
                  id="name"
                  v-model="form.name"
                  class="mt-2"
                  autocomplete="name"
                  placeholder="你的名字"
                  :invalid="!!errors.name"
                  describedby="name-error"
                />
                <p v-if="errors.name" id="name-error" class="mt-1.5 text-xs text-red-600">{{ errors.name }}</p>
              </div>

              <div>
                <label for="email" class="text-sm font-medium text-foreground">邮箱 *</label>
                <Input
                  id="email"
                  v-model="form.email"
                  type="email"
                  class="mt-2"
                  autocomplete="email"
                  placeholder="name@company.com"
                  :invalid="!!errors.email"
                  describedby="email-error"
                />
                <p v-if="errors.email" id="email-error" class="mt-1.5 text-xs text-red-600">{{ errors.email }}</p>
              </div>

              <div class="sm:col-span-2">
                <label for="company" class="text-sm font-medium text-foreground">公司 / 团队</label>
                <Input id="company" v-model="form.company" class="mt-2" autocomplete="organization" placeholder="选填" />
              </div>

              <div class="sm:col-span-2">
                <label for="message" class="text-sm font-medium text-foreground">想解决的问题 *</label>
                <Textarea
                  id="message"
                  v-model="form.message"
                  class="mt-2"
                  :rows="5"
                  placeholder="例如：团队 30 人，文档散在三个工具里，想统一检索……"
                  :invalid="!!errors.message"
                  describedby="message-error"
                />
                <p v-if="errors.message" id="message-error" class="mt-1.5 text-xs text-red-600">{{ errors.message }}</p>
              </div>
            </div>

            <Button type="submit" size="lg" class="mt-6 w-full sm:w-auto">提交</Button>
          </form>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
/** 联系页：联系信息 + 可访问表单（标签、就近错误、aria-invalid、成功态 role=status） */
import { reactive, ref } from 'vue'
import { Clock, Mail, MapPin, Phone } from 'lucide-vue-next'
import { site } from '@/content/site'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Textarea from '@/components/ui/Textarea.vue'

const form = reactive({ name: '', email: '', company: '', message: '' })
const errors = reactive({ name: '', email: '', message: '' })
const submitted = ref(false)

const contacts = [
  { label: '邮箱', value: site.contact.email, href: `mailto:${site.contact.email}`, icon: Mail },
  { label: '电话', value: site.contact.phone, href: `tel:${site.contact.phone.replace(/\s/g, '')}`, icon: Phone },
  { label: '地址', value: site.contact.address, icon: MapPin },
  { label: '服务时间', value: site.contact.hours, icon: Clock },
]

function validate() {
  errors.name = form.name.trim() ? '' : '请填写称呼'
  errors.email = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()) ? '' : '请填写有效邮箱'
  errors.message = form.message.trim().length >= 10 ? '' : '请至少写 10 个字，方便我们理解问题'
  return !errors.name && !errors.email && !errors.message
}

function submit() {
  if (!validate()) return
  // 模版默认不做网络请求；接入后端时在这里调用你的 API
  submitted.value = true
}

function reset() {
  submitted.value = false
  form.name = ''
  form.email = ''
  form.company = ''
  form.message = ''
}
</script>
