/**
 * 令牌本地存储工具
 * 约定沿用旧项目：令牌以 "admintem_token" 为 key 保存；
 * 请求时由 axios 拦截器自动附加请求头 admintem_token: "LAUNCH <token>"
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
