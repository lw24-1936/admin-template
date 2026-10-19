/**
 * axios 请求封装
 * - 自动附加认证请求头：admintem_token: "LAUNCH <token>"（与旧项目约定一致）
 * - 统一处理响应：HTTP 401 -> 清理本地登录态并跳转登录页（并发 401 只跳一次）
 * - 业务码与 HTTP 码分开处理：业务异常为 HTTP 200 + code != 200，安全相关异常
 *   （401/403）返回真实 HTTP 状态码；两者都统一抛出可读错误并提示一次
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'
import router from '@/router'

// 创建 axios 实例：baseURL 来自 .env 中 VITE_API_BASE_URL
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
})

/** 防止 401 时多个请求同时触发多次跳转 */
let redirectingToLogin = false

// 请求拦截器：附加令牌
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['admintem_token'] = 'LAUNCH ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一错误处理
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一返回 { code, message, data }
    if (res && typeof res.code !== 'undefined' && res.code !== 200) {
      ElMessage.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    }
    return res
  },
  (error) => {
    const status = error.response ? error.response.status : 0
    // 退出登录本身是“清会话”语义：令牌可能已失效，此时后端返回 401，
    // 不应再弹“登录已过期”或再次跳转登录页（用户正在离开系统）
    const isLogoutRequest = String(error.config?.url || '').includes('/auth/logout')
    if (status === 401 && isLogoutRequest) {
      return Promise.reject(error)
    }
    // 后端异常统一返回 { code, message, data }；data 可能是字符串或 HTML，非对象时不做取值
    const data = error.response ? error.response.data : null
    let msg = '网络异常，请稍后重试'
    if (data && typeof data === 'object' && typeof data.message === 'string' && data.message) {
      msg = data.message
    }
    if (status === 401) {
      // 未登录/令牌失效：清理本地登录态并跳转登录页（仅跳一次，避免并发 401 多跳）
      msg = '登录已过期，请重新登录'
      if (!redirectingToLogin) {
        redirectingToLogin = true
        removeToken()
        const current = router.currentRoute.value
        if (current.path !== '/login' && current.path !== '/register') {
          router.replace({
            path: '/login',
            query: { redirect: current.fullPath },
          })
        }
        setTimeout(() => {
          redirectingToLogin = false
        }, 800)
      }
    } else if (status === 403) {
      msg = data && data.message ? data.message : '没有操作权限'
    } else if (status >= 500) {
      // 后端未给出可读 message 时的兜底，避免提示“网络异常”掩盖服务端故障
      msg = data && data.message ? data.message : '服务器异常，请稍后重试'
    } else if (status === 0) {
      // 无响应：区分“超时”与“连不上服务”
      if (error.code === 'ECONNABORTED' || String(error.message).includes('timeout')) {
        msg = '请求超时，请稍后重试'
      }
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default service
