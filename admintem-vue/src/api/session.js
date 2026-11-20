/**
 * 在线会话接口。
 *
 * 与后端 `SysAuthSessionController` 对应：
 *   POST /sysAuthSession/queryOnline  在线列表（需要菜单权限 onlineSession:onlineSession）
 *   POST /sysAuthSession/kick         强制下线（需要按钮权限 sysAuthSession:kick）
 */
import request from '@/utils/request'

/**
 * 查询在线会话。
 *
 * @returns {Promise<object>} { code, message, data: OnlineSessionVO[] }
 */
export function queryOnlineSessions() {
  return request.post('/sysAuthSession/queryOnline')
}

/**
 * 强制指定账号下线（删除其令牌白名单，下一次请求即被拒绝）。
 *
 * @param {string} userName 目标账号
 * @returns {Promise<object>} { code, message }
 */
export function kickSession(userName, deviceId) {
  return request.post('/sysAuthSession/kick', { userName, deviceId })
}
