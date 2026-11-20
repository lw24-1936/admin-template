/**
 * 令牌本地存储工具
 * 约定沿用旧项目：令牌以 "admintem_token" 为 key 保存；
 * 请求时由 axios 拦截器自动附加请求头 admintem_token: "LAUNCH <token>"
 */

const TOKEN_KEY = 'admintem_token'
const REFRESH_TOKEN_KEY = 'admintem_refresh_token'

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

/** 读取刷新令牌 */
export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || ''
}

/** 保存刷新令牌 */
export function setRefreshToken(token) {
  if (token) localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

/** 清除刷新令牌 */
export function removeRefreshToken() {
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

/**
 * 获取/生成本机设备 ID（首次访问时创建并持久化）。
 * 多端同时登录时后端按 deviceId 区分访问令牌；不传则回落旧版单会话。
 */
export function getDeviceId() {
  const key = 'admintem_device_id'
  const existing = localStorage.getItem(key)
  if (existing) return existing
  const random = typeof globalThis.crypto !== 'undefined' && globalThis.crypto.randomUUID
    ? globalThis.crypto.randomUUID()
    : Math.random().toString(36).slice(2) + Date.now().toString(36)
  const value = 'web-' + random.replace(/-/g, '').slice(0, 40)
  localStorage.setItem(key, value)
  return value
}

