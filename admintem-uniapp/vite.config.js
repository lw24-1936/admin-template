import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

/**
 * uni-app（Vue 3 + Vite）开发/构建配置
 *
 * 与后端联调方式：
 * - H5：开发服务器把 `/api` 反代到 admintem-backend（8090），前端只写相对路径，
 *   避免浏览器跨域；生产部署时由 Nginx 做同源反代或直接配 VITE_API_BASE_URL。
 * - 小程序：没有代理概念，请求走 `src/config/index.js` 里的绝对地址
 *   （默认 http://localhost:8090，真机调试需改成局域网 IP 并在开发者工具里关闭域名校验）。
 */
export default defineConfig({
  plugins: [uni()],
  // uni-app 的 app / 小程序构建不会把 VITE_* 注入 import.meta.env（只给 H5 用），
  // 这里显式 define 一个常量，三端都能在构建时覆盖后端地址：
  //   VITE_API_BASE_URL=http://192.168.1.214:8090 pnpm build:app
  define: {
    __ADMINTEM_API_BASE__: JSON.stringify(process.env.VITE_API_BASE_URL || ''),
  },
  server: {
    port: 5176,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // 头像等上传文件的静态资源也由后端提供（/uploads/...）
      '/uploads': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
    },
  },
})
