/**
 * 认证相关接口（与桌面端共用同一套后端接口）
 */
import request from '@/utils/request'

/** 登录 */
export function login(data) {
  return request({ url: '/auth/login', method: 'post', data })
}

/** 注册（是否开放由后端 register-enabled 控制） */
export function register(data) {
  return request({ url: '/auth/register', method: 'post', data })
}

/** 退出登录 */
export function logout() {
  return request({ url: '/auth/logout', method: 'post' })
}

/** 获取当前用户会话（用户/角色/菜单/权限） */
export function getUserInfo() {
  return request({ url: '/auth/getUserInfo', method: 'post' })
}

/** 修改本人密码 */
export function updatePassword(data) {
  return request({ url: '/auth/updatePassword', method: 'post', data })
}
