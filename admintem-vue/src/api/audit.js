/**
 * 操作审计接口（只读）。
 *
 * 与后端 `SysAuthAuditLogController` 对应：
 *   POST /sysAuthAuditLog/paginQuery   分页查询（需要按钮权限 sysAuthAuditLog:paginQuery）
 *   POST /sysAuthAuditLog/queryActions 动作字典（复用同一权限）
 */
import request from '@/utils/request'

/**
 * 分页查询审计记录。
 *
 * @param {object} params 查询条件（pageNum/pageSize/operator/action/targetName/result/beginTime/endTime）
 * @returns {Promise<object>} { code, message, data: { records, total, pageNum, pageSize } }
 */
export function pageAuditLogs(params) {
  return request.post('/sysAuthAuditLog/paginQuery', params)
}

/**
 * 动作字典：已出现过的动作码与中文名（按出现次数倒序）。
 *
 * @returns {Promise<object>} { code, message, data: [{ action, actionName, total }] }
 */
export function queryActionDict() {
  return request.post('/sysAuthAuditLog/queryActions')
}

/** 导出审计 CSV（最多 500 行，返回 Blob） */
export function exportAuditLogs(params) {
  return request({ url: '/sysAuthAuditLog/export', method: 'post', data: params, responseType: 'blob' })
}
