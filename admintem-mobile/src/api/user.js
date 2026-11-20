import request from '@/utils/request'

/** 移动端用户列表（只读查询 + 启用/禁用）。 */
export function pageUsers(data) {
  return request.post('/sysAuthUser/paginQuery', data)
}

export function batchUserStatus(ids, status) {
  return request.post('/sysAuthUser/batchStatus', { ids, status })
}
