import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

/**
 * Vite 配置（桌面端）
 *
 * 相对上一版的三处调整（B4）：
 *   1. vue-devtools 只在 dev server 生效：生产构建不再加载该插件（它是调试工具，
 *      不应出现在用户浏览器里）；
 *   2. 显式拆分 vendor chunk：框架与组件库单独成块，业务代码更新时用户不必重新下载
 *      vue / element-plus（这两块体积大、版本变化频率远低于业务代码）；
 *   3. chunk 告警阈值回到默认量级（500KB）：此前抬到 1500 相当于把"包太大"的告警关掉了。
 */
export default defineConfig(({ command }) => ({
  plugins: [
    vue(),
    // 仅 dev server 加载（command 只有 serve / build 两种）
    ...(command === 'serve' ? [vueDevTools()] : []),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0', // 允许局域网访问（可按需收紧）
    port: 5173,
    open: false,
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 500,
    rollupOptions: {
      output: {
        // 注意：Vite 8 底层是 Rolldown，manualChunks 只接受**函数**形式
        // （对象形式会报 "manualChunks is not a function"）。
        // 这里按模块 id 归类：框架 / 组件库 / 其余第三方，业务代码保持默认切分。
        manualChunks(id) {
          const path = id.replace(/\\/g, '/')
          if (!path.includes('node_modules')) return undefined
          if (path.includes('element-plus') || path.includes('@element-plus')) {
            return 'vendor-element'
          }
          if (
            path.includes('/vue/') ||
            path.includes('vue-router') ||
            path.includes('pinia') ||
            path.includes('@vue/')
          ) {
            return 'vendor-vue'
          }
          return 'vendor'
        },
      },
    },
  },
}))
