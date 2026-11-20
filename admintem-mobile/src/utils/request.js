/**
 * axios 请求封装（移动端）
 *
 * - 自动附加认证请求头：admintem_token: "LAUNCH <token>"
 * - 响应统一处理：HTTP 401 清理登录态并跳转登录页（并发 401 只跳一次）
 * - 业务错误使用 Vant Toast 提示，避免与桌面端 ElMessage 耦合
 * - 失败一律记日志（方法 + 路径 + 状态码 + traceId），便于在手机上定位问题；
 *   日志不含令牌与请求体，避免把敏感信息写进控制台或日志采集
 */
import axios from 'axios'
import { showFailToast } from 'vant'
import { getToken, setToken, removeToken, getRefreshToken, setRefreshToken, removeRefreshToken } from '@/utils/auth'
import { error as logError, warn } from '@/utils/logger'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
})

/** 防止 401 时多个请求同时触发多次跳转 */
let redirectingToLogin = false
/** 并发 401 共享同一次刷新请求 */
let refreshPromise = null

function refreshAccessToken() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return Promise.reject(new Error('无刷新令牌'))
  if (!refreshPromise) {
    const baseURL = import.meta.env.VITE_API_BASE_URL || ''
    refreshPromise = axios
      .post(`${baseURL}/auth/refresh`, { refreshToken })
      .then((response) => {
        const data = response.data && response.data.data
        if (!data || !data.token || !data.refreshToken) throw new Error('刷新令牌响应无效')
        setToken(data.token)
        setRefreshToken(data.refreshToken)
        return data.token
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

function redirectToLogin() {
  removeToken()
  removeRefreshToken()
  if (redirectingToLogin) return
  redirectingToLogin = true
  const current = router.currentRoute.value
  if (current.path !== '/login') {
    router.replace({ path: '/login', query: { redirect: current.fullPath } })
  }
  setTimeout(() => {
    redirectingToLogin = false
  }, 800)
}

/** 从响应头取后端链路 ID（RequestTraceFilter 写入，便于与后端日志对齐） */
function traceIdOf(response) {
  if (!response || !response.headers) return ''
  return response.headers['x-trace-id'] || ''
}

// 请求拦截器：附加令牌
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['admintem_token'] = 'LAUNCH ' + token
    }
    return config
  },
  (error) => {
    logError('请求发送失败:', error && error.message)
    return Promise.reject(error)
  }
)

// 响应拦截器：统一错误处理
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一返回 { code, message, data }
    if (res && typeof res.code !== 'undefined' && res.code !== 200) {
      const method = (response.config && response.config.method) || ''
      const url = (response.config && response.config.url) || ''
      warn('业务失败:', method.toUpperCase(), url, 'code=' + res.code, res.message || '')
      showFailToast(res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    }
    return res
  },
  (error) => {
    const status = error.response ? error.response.status : 0
    const method = (error.config && error.config.method) || ''
    const url = (error.config && error.config.url) || ''
    const traceId = traceIdOf(error.response)
    const suffix = traceId ? ' traceId=' + traceId : ''

    const originalRequest = error.config || {}
    const requestUrl = String(originalRequest.url || '')
    const canRefresh = status === 401
      && !originalRequest._retry
      && !requestUrl.includes('/auth/login')
      && !requestUrl.includes('/auth/refresh')
      && !!getRefreshToken()
    if (canRefresh) {
      originalRequest._retry = true
      return refreshAccessToken()
        .then((token) => {
          originalRequest.headers = originalRequest.headers || {}
          originalRequest.headers['admintem_token'] = 'LAUNCH ' + token
          return service(originalRequest)
        })
        .catch(() => {
          redirectToLogin()
          showFailToast('登录已过期，请重新登录')
          return Promise.reject(error)
        })
    }

    let msg = '网络异常，请稍后重试'
    if (error.response && error.response.data && error.response.data.message) {
      msg = error.response.data.message
    }
    if (status === 401) {
      warn('登录态失效:', method.toUpperCase(), url + suffix)
      msg = '登录已过期，请重新登录'
      redirectToLogin()
    } else if (status === 403) {
      warn('无权限:', method.toUpperCase(), url + suffix)
      msg =
        error.response && error.response.data && error.response.data.message
          ? error.response.data.message
          : '没有操作权限'
    } else if (status >= 500) {
      logError('服务端异常:', method.toUpperCase(), url, 'status=' + status + suffix)
      msg = (error.response && error.response.data && error.response.data.message) || '服务器异常，请稍后重试'
    } else if (status === 0) {
      // 无响应：区分"超时"与"连不上服务"（移动网络下两种情况都常见）
      if (error.code === 'ECONNABORTED' || String(error.message || '').includes('timeout')) {
        logError('请求超时:', method.toUpperCase(), url + suffix)
        msg = '请求超时，请检查网络后重试'
      } else {
        logError('网络不可达:', method.toUpperCase(), url, (error.message || '') + suffix)
      }
    } else {
      warn('请求失败:', method.toUpperCase(), url, 'status=' + status + suffix)
    }
    showFailToast(msg)
    return Promise.reject(error)
  }
)

export default service
