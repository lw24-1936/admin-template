<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <button type="button" class="locale-switch" :aria-label="t('language.switch')" :title="t('language.switch')">
      <el-icon><Switch /></el-icon>
      <span>{{ locale === 'en-US' ? 'EN' : '中' }}</span>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh-CN" :disabled="locale === 'zh-CN'">简体中文</el-dropdown-item>
        <el-dropdown-item command="en-US" :disabled="locale === 'en-US'">English</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { Switch } from '@element-plus/icons-vue'
import { useI18n } from '@/i18n'

const { locale, t, setLocale } = useI18n()

function handleCommand(value) {
  setLocale(value)
}
</script>

<style scoped>
.locale-switch {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 8px;
  color: var(--lc-ink-muted);
  background: transparent;
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-ctl);
  cursor: pointer;
  transition: color var(--lc-motion-fast) var(--lc-ease-out),
    border-color var(--lc-motion-fast) var(--lc-ease-out);
}

.locale-switch:hover {
  color: var(--lc-brand);
  border-color: var(--lc-brand-ring);
}
</style>
