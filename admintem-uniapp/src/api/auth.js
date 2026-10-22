/**
 * 认证接口（与桌面端/移动端共用同一套后端）
 *
 * 后端契约（admintem-backend AuthController）：
 *   POST /auth/login            { username, password, rememberMe } -> { token, user }
 *   POST /auth/register         { userName, nickName, password, email, phone, sex }
 *   POST /auth/logout
 *   POST /auth/getUserInfo      -> { userId, userName, nickName, avatar, email, phone, sex, introduce, status, roles, menus, permissions }
 *   POST /auth/updateProfile    { nickName, email, phone, sex, avatar, introduce }
 *   POST /auth/updatePassword   { oldPassword, newPassword }
 *   POST /auth/uploadAvatar     multipart file -> 相对路径
 */
import request, { upload } from '@/utils/request'

/** 登录 */
export function login(data) {
  return request({ url: '/auth/login', method: 'POST', data })
}

/** 注册（是否开放由后端 register-enabled 控制） */
export function register(data) {
  return request({ url: '/auth/register', method: 'POST', data })
}

/** 退出登录（服务端令牌白名单立即失效） */
export function logout() {
  return request({ url: '/auth/logout', method: 'POST', silent: true })
}

/** 拉取当前会话（角色/菜单/权限） */
export function getUserInfo() {
  return request({ url: '/auth/getUserInfo', method: 'POST' })
}

/** 修改本人资料 */
export function updateProfile(data) {
  return request({ url: '/auth/updateProfile', method: 'POST', data })
}

/** 修改本人密码（成功后需重新登录） */
export function updatePassword(data) {
  return request({ url: '/auth/updatePassword', method: 'POST', data })
}

/** 上传头像，返回后端相对路径（如 /uploads/avatar/...） */
export function uploadAvatar(filePath) {
  return upload({ url: '/auth/uploadAvatar', filePath })
}
