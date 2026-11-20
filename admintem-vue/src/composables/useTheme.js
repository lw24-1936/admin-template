import { ref, watch } from 'vue'

const THEME_KEY = 'admintem_theme'
const stored = typeof localStorage !== 'undefined' ? localStorage.getItem(THEME_KEY) : null
export const theme = ref(stored === 'dark' ? 'dark' : 'light')

/** 应用主题到根节点：data-theme 供自有 token 使用，dark 类供 Element Plus 使用 */
export function applyTheme(value = theme.value) {
  if (typeof document === 'undefined') return
  document.documentElement.dataset.theme = value
  document.documentElement.classList.toggle('dark', value === 'dark')
}

export function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
}

watch(theme, (value) => {
  applyTheme(value)
  try {
    localStorage.setItem(THEME_KEY, value)
  } catch {
    // 隐私模式下写入失败不影响当前主题
  }
})

applyTheme()
