import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * 移动端工程构建配置
 * - 开发端口 5174（与桌面端 5173 区分，可同时运行）
 * - 允许局域网访问，便于真机联调
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5174,
    open: false,
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1200,
  },
})
