/**
 * 本地存储封装（令牌 + 会话缓存 + 记住账号）
 *
 * uni-app 在小程序里没有 localStorage，必须用 uni.setStorageSync；
 * H5 下它内部落到 localStorage，所以三端可以用同一套代码。
 */
import { STORAGE_KEYS } from '@/config'

export function getToken() {
  try {
    return uni.getStorageSync(STORAGE_KEYS.token) || ''
  } catch (e) {
    return ''
  }
}

export function getRefreshToken() {
  try {
    return uni.getStorageSync(STORAGE_KEYS.refreshToken) || ''
  } catch (e) {
    return ''
  }
}

export function setRefreshToken(token) {
  try {
    uni.setStorageSync(STORAGE_KEYS.refreshToken, token || '')
  } catch (e) {
    // 存储失败不阻断登录
  }
}

export function setToken(token) {
  try {
    uni.setStorageSync(STORAGE_KEYS.token, token || '')
  } catch (e) {
    // 存储满/隐私模式：失败不阻断登录流程，只是刷新后需要重新登录
  }
}

/** 会话缓存：用于首屏不用等接口就能渲染昵称/头像 */
export function getCachedUser() {
  try {
    return uni.getStorageSync(STORAGE_KEYS.user) || null
  } catch (e) {
    return null
  }
}

export function setCachedUser(user) {
  try {
    uni.setStorageSync(STORAGE_KEYS.user, user || null)
  } catch (e) {
    // 同上：缓存失败不影响主流程
  }
}

export function getRememberedName() {
  try {
    return uni.getStorageSync(STORAGE_KEYS.remember) || ''
  } catch (e) {
    return ''
  }
}

export function setRememberedName(name) {
  try {
    uni.setStorageSync(STORAGE_KEYS.remember, name || '')
  } catch (e) {
    // ignore
  }
}

/** 清空登录态（令牌 + 会话缓存），退出登录与 401 都走这里 */
export function clearSession() {
  try {
    uni.removeStorageSync(STORAGE_KEYS.token)
    uni.removeStorageSync(STORAGE_KEYS.user)
  } catch (e) {
    // ignore
  }
}
