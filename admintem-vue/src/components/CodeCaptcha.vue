<template>
  <div class="code-captcha" :data-code="debugText" :data-state="state">
    <div class="cap-row">
      <input
        v-model="input"
        class="cap-input"
        :maxlength="length"
        placeholder="请输入验证码"
        autocomplete="off"
        spellcheck="false"
        @input="onInput"
        @keyup.enter="submit"
      />
      <img
        v-if="image"
        class="cap-img"
        :src="image"
        alt="图形验证码"
        draggable="false"
        @click="load"
      />
      <button class="cap-link" type="button" @click="load">看不清？换一张</button>
    </div>
    <p v-if="state !== 'idle'" class="cap-status" :class="{ 'cap-status--ok': state === 'ok' }">
      {{ tip }}
    </p>
  </div>
</template>

<script setup>
/**
 * 图形验证码（数字 + 字母）
 *
 * 一行布局：输入框 → 验证码图片 → “看不清？换一张”。
 * 图片由服务端渲染，答案只在服务端 Redis；输入满位数自动校验，
 * 通过后把一次性 ticket 交给登录页。debug 模式（仅 dev）带 data-code 供 E2E 使用。
 */
import { onMounted, ref } from 'vue'
import { getCaptcha, verifyCaptcha } from '@/api/auth'

const emit = defineEmits(['success'])
const image = ref('')
const token = ref('')
const length = ref(4)
const debugText = ref(null)
const input = ref('')
const state = ref('loading')
const tip = ref('')

let tokenId = 0

async function load() {
  const id = ++tokenId
  state.value = 'loading'
  input.value = ''
  tip.value = ''
  try {
    const res = await getCaptcha()
    if (id !== tokenId) return
    const data = res.data || {}
    image.value = data.image
    token.value = data.token
    length.value = data.length || 4
    debugText.value = data.debugText || null
    state.value = 'idle'
  } catch {
    state.value = 'error'
    tip.value = '验证码加载失败，请点“看不清？换一张”重试'
  }
}

function onInput() {
  // 输入满位数自动提交，减少一次点击
  if (state.value === 'idle' && input.value.length >= length.value) {
    submit()
  }
}

async function submit() {
  if (state.value !== 'idle' || !input.value) return
  state.value = 'checking'
  tip.value = '校验中…'
  try {
    const res = await verifyCaptcha({ token: token.value, code: input.value })
    state.value = 'ok'
    tip.value = '验证通过'
    emit('success', res.data.ticket)
  } catch {
    state.value = 'fail'
    tip.value = '验证码不正确，正在刷新…'
    setTimeout(load, 700)
  }
}

function reset() {
  load()
}

onMounted(load)
defineExpose({ reset })
</script>

<style scoped>
.code-captcha {
  width: 100%;
}

/* 输入框 + 图片 + 换一张，同一行 */
.cap-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.cap-input {
  flex: 1;
  min-width: 0;
  height: 44px;
  padding: 0 12px;
  border: 1px solid var(--lc-line-input, var(--lc-line));
  border-radius: 8px;
  background: var(--lc-paper-card);
  color: var(--lc-ink);
  font-size: 14px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  outline: none;
}

.cap-input:focus {
  border-color: var(--lc-brand);
}

.cap-img {
  width: 128px;
  height: 44px;
  flex: 0 0 128px;
  border: 1px solid var(--lc-line);
  border-radius: 8px;
  background: var(--lc-surface-soft);
  cursor: pointer;
}

.cap-link {
  flex: 0 0 auto;
  padding: 0;
  border: 0;
  background: none;
  color: var(--lc-brand);
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
  cursor: pointer;
}

.cap-link:hover {
  text-decoration: underline;
}

.cap-status {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--lc-ink-muted);
}

.cap-status--ok {
  color: var(--lc-success);
}
</style>
