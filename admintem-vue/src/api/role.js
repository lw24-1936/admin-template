/**
 * 角色管理接口（对应后端 /sysAuthRole、/sysAuthRolePermission）
 */
import request from '@/utils/request'

/** 查询全部角色（下拉选项） */
export function queryAllRoles() {
  return request({ url: '/sysAuthRole/queryAll', method: 'get' })
}

/** 分页查询角色 */
export function pageRoles(data) {
  return request({ url: '/sysAuthRole/paginQuery', method: 'post', data })
}

/** 新增角色 */
export function addRole(data) {
  return request({ url: '/sysAuthRole/addAuthRole', method: 'post', data })
}

/** 编辑角色 */
export function editRole(data) {
  return request({ url: '/sysAuthRole/editAuthRole', method: 'post', data })
}

/** 删除角色 */
export function deleteRole(id) {
  return request({ url: '/sysAuthRole/deleteById', method: 'post', data: { id } })
}

/** 分配角色权限 */
export function assignRolePermissions(data) {
  return request({ url: '/sysAuthRole/assignPermission', method: 'post', data })
}

/** 查询角色已分配权限（含权限树） */
export function getRolePermissions(roleId) {
  return request({ url: '/sysAuthRolePermission/getPermissionByRoleId', method: 'post', data: { roleId } })
}
