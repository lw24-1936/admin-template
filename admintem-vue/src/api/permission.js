/**
 * 权限管理接口（对应后端 /sysAuthPermission）
 */
import request from '@/utils/request'

/** 查询权限树 */
export function treePermissions(includeDisabled = true) {
  return request({ url: '/sysAuthPermission/treeList', method: 'get', params: { includeDisabled } })
}

/** 新增权限（菜单/按钮） */
export function addPermission(data) {
  return request({ url: '/sysAuthPermission/addAuthPermission', method: 'post', data })
}

/** 编辑权限 */
export function editPermission(data) {
  return request({ url: '/sysAuthPermission/editAuthPermission', method: 'post', data })
}

/** 删除权限 */
export function deletePermission(id) {
  return request({ url: '/sysAuthPermission/deleteById', method: 'post', data: { id } })
}
