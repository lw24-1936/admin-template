<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :close-on-click-modal="false"
    destroy-on-close
    append-to-body
  >
    <slot />
    <template #footer>
      <slot name="footer">
        <el-button @click="close">取消</el-button>
        <el-button type="primary" :loading="loading" @click="$emit('confirm')">确定</el-button>
      </slot>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  width: { type: String, default: '520px' },
  loading: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

function close() {
  visible.value = false
}
</script>
