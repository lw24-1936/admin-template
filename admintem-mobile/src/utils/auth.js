/**
 * 令牌本地存储工具（与桌面端约定一致）
 * 保存 key 为 admintem_token，请求时由 axios 拦截器附加请求头 admintem_token: "LAUNCH <token>"
 */

const TOKEN_KEY = 'admintem_token'

/** 读取令牌 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/** 保存令牌 */
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 清除令牌 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}
