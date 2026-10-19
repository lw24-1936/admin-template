/**
 * 用户管理接口（对应后端 /sysAuthUser）
 */
import request from '@/utils/request'

/** 分页查询用户 */
export function pageUsers(data) {
  return request({ url: '/sysAuthUser/paginQuery', method: 'post', data })
}

/** 新增用户 */
export function addUser(data) {
  return request({ url: '/sysAuthUser/addSysAuthUser', method: 'post', data })
}

/** 编辑用户 */
export function editUser(data) {
  return request({ url: '/sysAuthUser/editSysAuthUser', method: 'post', data })
}

/** 删除用户 */
export function deleteUser(id) {
  return request({ url: '/sysAuthUser/deleteById', method: 'post', data: { id } })
}

/** 重置用户密码 */
export function resetUserPassword(data) {
  return request({ url: '/sysAuthUser/resetPassword', method: 'post', data })
}

/** 分配用户角色 */
export function assignUserRoles(data) {
  return request({ url: '/sysAuthUser/assignRole', method: 'post', data })
}

/** 查询用户已分配角色 */
export function getUserRoles(userId) {
  return request({ url: '/sysAuthUserRole/getUserRoleByUserId', method: 'post', data: { userId } })
}
