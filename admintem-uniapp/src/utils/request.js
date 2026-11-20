/**
 * 请求封装（uni.request）
 *
 * 与桌面端/移动端保持一致的三条契约：
 *   1. 认证头：`admintem_token: LAUNCH <token>`；
 *   2. 业务响应：`{ code, message, data }`，code !== 200 视为业务失败；
 *   3. 401：清空登录态并回到登录页（并发 401 只跳一次）。
 *
 * 小程序没有 axios，这里返回 Promise，调用方 `const res = await request(...)`
 * 后直接取 `res.data`，与移动端的 `res.data` 用法一致。
 */
import { API_BASE_URL, TOKEN_HEADER, TOKEN_PREFIX } from '@/config'
import { getToken, getRefreshToken, setToken, setRefreshToken, clearSession } from '@/utils/storage'

/** 防抖：多个请求同时 401 时只跳一次登录页 */
let redirecting = false
/** 并发 401 共用一次刷新请求 */
let refreshPromise = null

/** 用 refreshToken 换新 access；成功后写回本地存储。 */
function refreshAccessToken() {
  if (refreshPromise) return refreshPromise
  const refreshToken = getRefreshToken()
  if (!refreshToken) return Promise.reject(new Error('no-refresh-token'))
  refreshPromise = new Promise((resolve, reject) => {
    uni.request({
      url: API_BASE_URL + '/auth/refresh',
      method: 'POST',
      data: { refreshToken },
      header: { 'Content-Type': 'application/json' },
      success: (res) => {
        const body = (res && res.data) || {}
        const data = body.data || {}
        if (res.statusCode === 200 && body.code === 200 && data.token) {
          setToken(data.token)
          if (data.refreshToken) setRefreshToken(data.refreshToken)
          resolve()
          return
        }
        reject(new Error('refresh-failed'))
      },
      fail: () => reject(new Error('refresh-network-error')),
    })
  }).finally(() => {
    refreshPromise = null
  })
  return refreshPromise
}

function toast(title) {
  if (!title) return
  uni.showToast({ title: String(title).slice(0, 40), icon: 'none', duration: 2200 })
}

function toLogin() {
  if (redirecting) return
  redirecting = true
  clearSession()
  const pages = getCurrentPages()
  const current = pages.length ? pages[pages.length - 1].route : ''
  if (current !== 'pages/login/index') {
    uni.reLaunch({ url: '/pages/login/index' })
  }
  setTimeout(() => {
    redirecting = false
  }, 800)
}

/**
 * 发起请求
 * @param {object} options
 * @param {string} options.url 以 / 开头的接口路径，如 /auth/login
 * @param {string} [options.method] 默认 GET
 * @param {object} [options.data]
 * @param {object} [options.header] 额外请求头
 * @param {boolean} [options.silent] 为 true 时失败不弹提示（页面自行处理）
 */
export function request({ url, method = 'GET', data, header = {}, silent = false, timeout = 30000, _retried = false }) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.request({
      url: API_BASE_URL + url,
      method,
      data,
      timeout,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { [TOKEN_HEADER]: TOKEN_PREFIX + token } : {}),
        ...header,
      },
      success: (res) => {
        const { statusCode, data: body, header: resHeader } = res || {}
        const traceId = (resHeader && (resHeader['x-trace-id'] || resHeader['X-Trace-Id'])) || ''

          if (statusCode === 401) {
            if (!_retried && getRefreshToken()) {
              refreshAccessToken()
                .then(() => request({ url, method, data, header, silent, timeout, _retried: true }))
                .then(resolve)
                .catch(() => {
                  toLogin()
                  if (!silent) toast('登录已过期，请重新登录')
                  reject(new Error('unauthorized' + (traceId ? ' traceId=' + traceId : '')))
                })
              return
            }
            toLogin()
            if (!silent) toast('登录已过期，请重新登录')
            reject(new Error('unauthorized' + (traceId ? ' traceId=' + traceId : '')))
            return
          }

        // 后端统一返回体；兼容 204 之类无 body 的成功响应
        if (statusCode >= 200 && statusCode < 300) {
          if (body && typeof body.code !== 'undefined' && body.code !== 200) {
            if (!silent) toast(body.message || '操作失败')
            reject(new Error(body.message || 'business-error'))
            return
          }
          resolve(body || { code: 200, data: null })
          return
        }

        const msg =
          (body && body.message) ||
          (statusCode === 403 ? '没有操作权限' : statusCode >= 500 ? '服务器异常，请稍后重试' : '请求失败')
        if (!silent) toast(msg)
        reject(new Error(`${statusCode} ${msg}${traceId ? ' traceId=' + traceId : ''}`))
      },
      fail: (err) => {
        const msg = String((err && err.errMsg) || '').includes('timeout')
          ? '请求超时，请检查网络后重试'
          : '网络异常，请稍后重试'
        if (!silent) toast(msg)
        reject(new Error(msg))
      },
    })
  })
}

/**
 * 上传文件（头像）：uni.uploadFile 同样需要手动带认证头
 * @param {object} options
 * @param {string} options.url 接口路径
 * @param {string} options.filePath 本地临时文件路径
 * @param {string} [options.name] 表单字段名，默认 file（后端 @RequestParam("file")）
 */
export function upload({ url, filePath, name = 'file' }) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.uploadFile({
      url: API_BASE_URL + url,
      filePath,
      name,
      header: token ? { [TOKEN_HEADER]: TOKEN_PREFIX + token } : {},
      success: (res) => {
        if (res.statusCode === 401) {
          toLogin()
          reject(new Error('unauthorized'))
          return
        }
        let body = null
        try {
          body = JSON.parse(res.data)
        } catch (e) {
          reject(new Error('上传响应解析失败'))
          return
        }
        if (body && body.code === 200) {
          resolve(body)
          return
        }
        toast((body && body.message) || '上传失败')
        reject(new Error((body && body.message) || 'upload-failed'))
      },
      fail: () => {
        toast('上传失败，请检查网络')
        reject(new Error('upload-failed'))
      },
    })
  })
}

export default request
