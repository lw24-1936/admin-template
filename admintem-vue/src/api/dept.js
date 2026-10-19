/**
 * 部门接口（V1.0.9）。
 *
 * 与后端 `SysAuthDeptController` 对应：
 *   POST /sysAuthDept/treeList   部门树（含各部门用户数，需要菜单权限 dept:dept）
 *   POST /sysAuthDept/addDept    新增（sysAuthDept:addDept）
 *   POST /sysAuthDept/editDept   编辑（sysAuthDept:editDept）
 *   POST /sysAuthDept/deleteById 删除（sysAuthDept:deleteById）
 */
import request from '@/utils/request'

/** 部门树 */
export function treeList() {
  return request.post('/sysAuthDept/treeList')
}

/**
 * 新增部门。
 *
 * @param {object} data { parentId, deptName, orderNum, leader, phone, status }
 */
export function addDept(data) {
  return request.post('/sysAuthDept/addDept', data)
}

/**
 * 编辑部门（id 必填）。
 *
 * @param {object} data { id, parentId, deptName, orderNum, leader, phone, status }
 */
export function editDept(data) {
  return request.post('/sysAuthDept/editDept', data)
}

/**
 * 删除部门（存在子部门或用户时后端会拒绝）。
 *
 * @param {number} id 部门ID
 */
export function deleteDept(id) {
  return request.post('/sysAuthDept/deleteById', { id })
}
