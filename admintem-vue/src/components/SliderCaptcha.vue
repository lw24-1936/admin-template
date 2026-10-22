<template>
  <div class="slider-captcha" :data-offset="debugOffset" :data-state="state">
    <div class="cap-stage" :style="{ width: width + 'px', height: height + 'px' }">
      <img v-if="background" class="cap-bg" :src="background" alt="" draggable="false" />
      <img
        v-if="piece"
        class="cap-piece"
        :src="piece"
        :style="{ top: pieceY + 'px', left: offset + 'px' }"
        alt=""
        draggable="false"
      />
      <div v-if="state === 'ok'" class="cap-ok">验证通过</div>
    </div>
    <div class="cap-track">
      <div class="cap-fill" :style="{ width: offset + 'px' }" />
      <div
        class="cap-handle"
        :style="{ left: offset + 'px' }"
        role="slider"
        :aria-valuenow="Math.round(offset)"
        aria-label="拖动完成拼图"
        @pointerdown="start"
      >
        <span class="cap-arrow">›</span>
      </div>
      <span class="cap-hint">{{ hint }}</span>
    </div>
  </div>
</template>

<script setup>
/**
 * 图形滑块验证码
 *
 * 服务端下发背景图与拼图块（SVG data URI），拖动位置由服务端校验；
 * 通过后拿到一次性票据，登录时回传。debug 模式（仅 dev）带 data-offset，供 E2E 拖拽。
 */
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCaptcha, verifyCaptcha } from '@/api/auth'

const emit = defineEmits(['success'])
const background = ref('')
const piece = ref('')
const width = ref(320)
const height = ref(160)
const pieceSize = ref(52)
const pieceY = ref(0)
const token = ref('')
const debugOffset = ref(null)
const offset = ref(0)
const state = ref('loading')
const hint = ref('拖动滑块完成拼图')

let dragging = false
let startX = 0
let startOffset = 0
let tokenId = 0

function maxOffset() {
  return Math.max(0, width.value - pieceSize.value)
}

async function load() {
  const id = ++tokenId
  state.value = 'loading'
  offset.value = 0
  hint.value = '加载验证码…'
  try {
    const res = await getCaptcha()
    if (id !== tokenId) return
    const data = res.data || {}
    background.value = data.background
    piece.value = data.piece
    width.value = data.width || 320
    height.value = data.height || 160
    pieceSize.value = data.pieceSize || 52
    pieceY.value = data.pieceY || 0
    token.value = data.token
    debugOffset.value = typeof data.debugOffset === 'number' ? data.debugOffset : null
    state.value = 'idle'
    hint.value = '拖动滑块完成拼图'
  } catch {
    state.value = 'error'
    hint.value = '验证码加载失败，点击重试'
  }
}

function start(e) {
  if (state.value !== 'idle') return
  dragging = true
  startX = e.clientX
  startOffset = offset.value
  e.target.setPointerCapture?.(e.pointerId)
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', end, { once: true })
}

function move(e) {
  if (!dragging) return
  offset.value = Math.min(maxOffset(), Math.max(0, startOffset + e.clientX - startX))
}

async function end() {
  window.removeEventListener('pointermove', move)
  if (!dragging) return
  dragging = false
  state.value = 'checking'
  hint.value = '校验中…'
  try {
    const res = await verifyCaptcha({ token: token.value, x: Math.round(offset.value) })
    state.value = 'ok'
    hint.value = '验证通过'
    emit('success', res.data.ticket)
  } catch {
    state.value = 'fail'
    hint.value = '位置不正确，正在刷新…'
    ElMessage.warning('拼图位置不正确，请重试')
    setTimeout(load, 600)
  }
}

function reset() {
  load()
}

onMounted(load)
defineExpose({ reset })
</script>

<style scoped>
.slider-captcha {
  width: 100%;
  user-select: none;
}
.cap-stage {
  position: relative;
  overflow: hidden;
  border: 1rpx solid var(--lc-line);
  border-radius: 12px;
  background: var(--lc-surface-soft);
}
.cap-bg {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cap-piece {
  position: absolute;
  width: 52px;
  height: 52px;
  border-radius: 8px;
  box-shadow: 0 6px 14px -6px rgba(15, 23, 42, 0.7);
  clip-path: inset(0 round 8px);
  pointer-events: none;
}
.cap-ok {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  letter-spacing: 0.1em;
  background: rgba(15, 23, 42, 0.45);
}
.cap-track {
  position: relative;
  height: 40px;
  margin-top: 10px;
  border: 1px solid var(--lc-line);
  border-radius: 8px;
  background: var(--lc-surface-soft);
  display: flex;
  align-items: center;
}
.cap-fill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 8px 0 0 8px;
  background: var(--lc-brand-soft);
}
.cap-handle {
  position: absolute;
  top: -1px;
  width: 46px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--lc-brand-deep);
  border-radius: 8px;
  background: var(--lc-brand);
  color: #fff;
  cursor: grab;
  touch-action: none;
  box-shadow: 0 8px 18px -12px var(--lc-brand-glow);
}
.cap-handle:active {
  cursor: grabbing;
}
.cap-arrow {
  font-size: 20px;
  line-height: 1;
}
.cap-hint {
  margin-left: auto;
  padding-right: 12px;
  font-size: 12px;
  color: var(--lc-ink-faint);
  pointer-events: none;
}
</style>
