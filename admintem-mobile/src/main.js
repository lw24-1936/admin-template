/**
 * admintem 移动端入口
 * - 注册 Pinia、路由
 * - Vant 采用**按需引入**：不再全量 app.use(Vant) 与全量样式，
 *   各页面自行 import 所需组件与其 style（vant/es/<组件>/style/index），
 *   从而显著减小 CSS/JS 体积；模板中的 <van-xxx> 标签通过 VanXxx 别名解析
 * - 令牌存储 key 与桌面端保持一致：admintem_token（请求头 admintem_token: "LAUNCH <token>"）
 * - 全局兜底：组件异常、未处理的 Promise 拒绝、window 错误都会记日志并给出可见提示，
 *   避免"白屏且无任何反馈"（移动端没有控制台，静默失败等于无法排查）
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { showFailToast } from 'vant'

// 轻提示与对话框为函数式调用（在 utils/request.js 与页面中直接 import 使用），
// 其样式不随组件模板引入，这里集中引入一次
import 'vant/es/toast/style/index'
import 'vant/es/dialog/style/index'

import App from '@/App.vue'
import router from '@/router'
import permissionDirective from '@/directives/permission'
import { error as logError, info } from '@/utils/logger'
// 样式引入顺序：设计 token（数值） -> 组件库变量映射 -> 全局样式
import '@/styles/tokens.css'
import '@/styles/theme.css'
import '@/assets/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
// 按钮级权限指令：v-perm="'userInfo:userInfo'"，无权限则移除元素（fail-closed）
app.directive('perm', permissionDirective)

/** 错误提示节流：同一屏内的连环异常只提示一次，避免叠一堆 Toast */
let lastToastAt = 0
function notifyOnce(message) {
  const now = Date.now()
  if (now - lastToastAt < 3000) return
  lastToastAt = now
  showFailToast(message)
}

// 1) 组件渲染/生命周期异常：记日志 + 轻提示（不吞异常，控制台仍可见堆栈）
app.config.errorHandler = (err, _instance, infoText) => {
  logError('组件异常:', infoText, err)
  notifyOnce('页面出现异常，请重试')
}

// 2) 未处理的 Promise 拒绝：最常见的"接口失败但没人 catch"
window.addEventListener('unhandledrejection', (event) => {
  logError('未处理的异步异常:', event.reason)
  event.preventDefault()
})

// 3) 资源加载/脚本运行时错误（含手机浏览器特有的兼容性报错）
window.addEventListener('error', (event) => {
  logError('全局错误:', event.message, event.filename, event.lineno)
})

info('移动端启动', { mode: import.meta.env.MODE })

app.mount('#app')
