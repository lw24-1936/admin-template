/**
 * 认证相关接口
 */
import request from '@/utils/request'
import { getDeviceId } from '@/utils/auth'

/** 生成图形验证码挑战 */
export function getCaptcha() {
  return request({ url: '/auth/captcha', method: 'post' })
}

/** 校验拼图位置，返回一次性票据 */
export function verifyCaptcha(data) {
  return request({ url: '/auth/captcha/verify', method: 'post', data })
}

/** 登录 */
export function login(data) {
  return request({ url: '/auth/login', method: 'post', data: { ...data, deviceId: getDeviceId() } })
}

/** 注册 */
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

/** 修改个人资料 */
export function updateProfile(data) {
  return request({ url: '/auth/updateProfile', method: 'post', data })
}

/** 修改本人密码 */
export function updatePassword(data) {
  return request({ url: '/auth/updatePassword', method: 'post', data })
}
/**
 * 上传并更新当前用户头像（multipart/form-data，字段名 file）。
 *
 * 校验在服务端：大小 ≤ 配置上限、扩展名白名单、文件头必须是真实图片。
 *
 * @param {File} file 图片文件
 * @returns {Promise<object>} { code, message, data: '/uploads/avatar/...' }
 */
export function uploadAvatar(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/auth/uploadAvatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}