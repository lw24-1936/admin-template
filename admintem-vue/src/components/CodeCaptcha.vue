<template>
  <div class="code-captcha" :data-code="debugText" :data-state="state">
    <div class="cap-row">
      <img v-if="image" class="cap-img" :src="image" alt="图形验证码" draggable="false" @click="load" />
      <button class="cap-refresh" type="button" @click="load">换一张</button>
    </div>
    <div class="cap-entry">
      <input
        v-model="input"
        class="cap-input"
        :maxlength="length"
        placeholder="请输入图中的数字与字母"
        autocomplete="off"
        spellcheck="false"
        @input="onInput"
        @keyup.enter="submit"
      />
      <span class="cap-tip" :class="{ 'cap-tip--ok': state === 'ok' }">{{ tip }}</span>
    </div>
  </div>
</template>

<script setup>
/**
 * 图形验证码（数字 + 字母）
 *
 * 图片由服务端渲染，答案只在服务端 Redis；输入满 length 位自动校验，
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
const tip = ref('加载中…')

let tokenId = 0

async function load() {
  const id = ++tokenId
  state.value = 'loading'
  input.value = ''
  tip.value = '加载中…'
  try {
    const res = await getCaptcha()
    if (id !== tokenId) return
    const data = res.data || {}
    image.value = data.image
    token.value = data.token
    length.value = data.length || 4
    debugText.value = data.debugText || null
    state.value = 'idle'
    tip.value = '看不清？点图片或换一张'
  } catch {
    state.value = 'error'
    tip.value = '加载失败，点击"换一张"重试'
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
.cap-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cap-img {
  width: 140px;
  height: 48px;
  cursor: pointer;
  border: 1px solid var(--lc-line);
  border-radius: 8px;
  background: var(--lc-surface-soft);
}
.cap-refresh {
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--lc-line);
  border-radius: 8px;
  background: var(--lc-paper-card);
  color: var(--lc-ink-muted);
  font-size: 12px;
  cursor: pointer;
}
.cap-refresh:hover {
  border-color: var(--lc-brand);
  color: var(--lc-brand);
}
.cap-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}
.cap-input {
  flex: 1;
  height: 40px;
  padding: 0 12px;
  border: 1px solid var(--lc-line-input, var(--lc-line));
  border-radius: 8px;
  background: var(--lc-paper-card);
  color: var(--lc-ink);
  font-size: 14px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  outline: none;
}
.cap-input:focus {
  border-color: var(--lc-brand);
}
.cap-tip {
  font-size: 12px;
  color: var(--lc-ink-faint);
  white-space: nowrap;
}
.cap-tip--ok {
  color: var(--lc-success);
}
</style>
