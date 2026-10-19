-- ============================================================================
-- admintem-backend 数据库增量变更脚本  V1.0.1
-- ----------------------------------------------------------------------------
-- 目标库   : admintem_new（与原项目共用同一 MySQL 库，勿在其它库执行）
-- 变更目的 :
--   1. 为"用户管理/角色管理/权限管理"补充按钮级权限(type=1)种子数据，
--      实现"菜单权限控页面 + 按钮权限控操作"的细粒度 RBAC；
--   2. 预置"普通用户(common)"角色，供自助注册默认分配；
--   3. 仅做【增量、幂等】插入，不改动任何存量记录；
--      历史遗留的 sys_auth_role_permission 孤儿记录(permission_id=7..11，
--      在 admintem_new 中无对应权限)保持原样，不做删除，避免影响旧系统。
-- 幂等性   : 全部使用 INSERT ... SELECT ... WHERE NOT EXISTS 写法，可重复执行。
-- 执行方式 :
--   mysql -h<host> -P3306 -uroot -p admintem_new < V1.0.1__init_button_permissions.sql
-- 对应文档 : docs/02-数据库设计与变更说明.md、docs/04-接口说明与权限标识对照表.md
-- 编写日期 : 2025-xx-xx（与后端 v1.0.0 配套发布）
-- ============================================================================

SET NAMES utf8mb4;
BEGIN;

-- ============================================================================
-- 一、补充按钮级权限（type=1）
-- ----------------------------------------------------------------------------
-- 约定：
--   parent_id     = 所属菜单的权限 ID（3 用户管理 / 4 角色管理 / 5 权限管理）
--   permission_key = 全局唯一权限标识：后端 @PreAuthorize 与前端 v-perm 指令共用
--   backend_url    = 对应的后端接口路径（审计/展示用）
--   status         = 0 启用
--   order_num      = 同级排序（沿用现有最大排序之后，避免与历史序号冲突）
-- ============================================================================

-- 1) 用户管理(id=3)下的操作按钮 --------------------------------------------
INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url, status, `show`, icon,
     permission_key, created_by, created_time, update_by, update_time, is_deleted)
SELECT '新增用户', 3, 10, 1, NULL, NULL, '/sysAuthUser/addSysAuthUser', 0, NULL, '',
       'sysAuthUser:addSysAuthUser', 'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_auth_permission
                  WHERE permission_key = 'sysAuthUser:addSysAuthUser' AND is_deleted = 0);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url, status, `show`, icon,
     permission_key, created_by, created_time, update_by, update_time, is_deleted)
SELECT '编辑用户', 3, 11, 1, NULL, NULL, '/sysAuthUser/editSysAuthUser', 0, NULL, '',
       'sysAuthUser:editSysAuthUser', 'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_auth_permission
                  WHERE permission_key = 'sysAuthUser:editSysAuthUser' AND is_deleted = 0);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url, status, `show`, icon,
     permission_key, created_by, created_time, update_by, update_time, is_deleted)
SELECT '删除用户', 3, 12, 1, NULL, NULL, '/sysAuthUser/deleteById', 0, NULL, '',
       'sysAuthUser:deleteById', 'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_auth_permission
                  WHERE permission_key = 'sysAuthUser:deleteById' AND is_deleted = 0);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url, status, `show`, icon,
     permission_key, created_by, created_time, update_by, update_time, is_deleted)
SELECT '重置密码', 3, 13, 1, NULL, NULL, '/sysAuthUser/resetPassword', 0, NULL, '',
       'sysAuthUser:resetPassword', 'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_auth_permission
                  WHERE permission_key = 'sysAuthUser:resetPassword' AND is_deleted = 0);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url, status, `show`, icon,
     permission_key, created_by, created_time, update_by, update_time, is_deleted)
SELECT '分配角色', 3, 14, 1, NULL, NULL, '/sysAuthUser/assignRole', 0, NULL, '',
       'sysAuthUser:assignRole', 'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_auth_permission
                  WHERE permission_key = 'sysAuthUser:assignRole' AND is_deleted = 0);

-- 2) 角色管理(id=4)下的操作按钮 --------------------------------------------
INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url, status, `show`, icon,
     permission_key, created_by, created_time, update_by, update_time, is_deleted)
SELECT '新增角色', 4, 10, 1, NULL, NULL, '/sysAuthRole/addAuthRole', 0, NULL, '',
       'sysAuthRole:addAuthRole', 'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_auth_permission
                  WHERE permission_key = 'sysAuthRole:addAuthRole' AND is_deleted = 0);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url, status, `show`, icon,
     permission_key, created_by, created_time, update_by, update_time, is_deleted)
SELECT '编辑角色', 4, 11, 1, NULL, NULL, '/sysAuthRole/editAuthRole', 0, NULL, '',
       'sysAuthRole:editAuthRole', 'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_auth_permission
                  WHERE permission_key = 'sysAuthRole:editAuthRole' AND is_deleted = 0);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url, status, `show`, icon,
     permission_key, created_by, created_time, update_by, update_time, is_deleted)
SELECT '删除角色', 4, 12, 1, NULL, NULL, '/sysAuthRole/deleteById', 0, NULL, '',
       'sysAuthRole:deleteById', 'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_auth_permission
                  WHERE permission_key = 'sysAuthRole:deleteById' AND is_deleted = 0);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url, status, `show`, icon,
     permission_key, created_by, created_time, update_by, update_time, is_deleted)
SELECT '分配权限', 4, 13, 1, NULL, NULL, '/sysAuthRole/assignPermission', 0, NULL, '',
       'sysAuthRole:assignPermission', 'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_auth_permission
                  WHERE permission_key = 'sysAuthRole:assignPermission' AND is_deleted = 0);

-- 3) 权限管理(id=5)下的操作按钮 --------------------------------------------
INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url, status, `show`, icon,
     permission_key, created_by, created_time, update_by, update_time, is_deleted)
SELECT '新增权限', 5, 10, 1, NULL, NULL, '/sysAuthPermission/addAuthPermission', 0, NULL, '',
       'sysAuthPermission:addAuthPermission', 'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_auth_permission
                  WHERE permission_key = 'sysAuthPermission:addAuthPermission' AND is_deleted = 0);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url, status, `show`, icon,
     permission_key, created_by, created_time, update_by, update_time, is_deleted)
SELECT '编辑权限', 5, 11, 1, NULL, NULL, '/sysAuthPermission/editAuthPermission', 0, NULL, '',
       'sysAuthPermission:editAuthPermission', 'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_auth_permission
                  WHERE permission_key = 'sysAuthPermission:editAuthPermission' AND is_deleted = 0);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url, status, `show`, icon,
     permission_key, created_by, created_time, update_by, update_time, is_deleted)
SELECT '删除权限', 5, 12, 1, NULL, NULL, '/sysAuthPermission/deleteById', 0, NULL, '',
       'sysAuthPermission:deleteById', 'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_auth_permission
                  WHERE permission_key = 'sysAuthPermission:deleteById' AND is_deleted = 0);

-- ============================================================================
-- 二、将新增按钮权限全量授权给"管理员(role_id=1)"
-- ----------------------------------------------------------------------------
-- 说明：管理员角色 role_key=admin 在后端属于超级角色（自动放行全部权限），
--       此处建立关联仅为让"角色授权"页面能正确回显勾选状态，保证管理体验一致。
-- ============================================================================
INSERT INTO sys_auth_role_permission (role_id, permission_id, created_by, created_time, is_deleted)
SELECT 1, p.id, 'admin', NOW(), 0
FROM sys_auth_permission p
WHERE p.permission_key IN (
      'sysAuthUser:addSysAuthUser', 'sysAuthUser:editSysAuthUser', 'sysAuthUser:deleteById',
      'sysAuthUser:resetPassword', 'sysAuthUser:assignRole',
      'sysAuthRole:addAuthRole', 'sysAuthRole:editAuthRole', 'sysAuthRole:deleteById',
      'sysAuthRole:assignPermission',
      'sysAuthPermission:addAuthPermission', 'sysAuthPermission:editAuthPermission',
      'sysAuthPermission:deleteById')
  AND NOT EXISTS (SELECT 1 FROM sys_auth_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id AND rp.is_deleted = 0);

-- ============================================================================
-- 三、预置"普通用户"角色（供自助注册默认分配）
-- ============================================================================
INSERT INTO sys_auth_role (role_name, role_key, created_by, created_time, update_by, update_time, is_deleted)
SELECT '普通用户', 'common', 'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_auth_role WHERE role_key = 'common' AND is_deleted = 0);

-- 普通用户仅授予"工作台"菜单（id=1，permission_key='home:homeview'），
-- 后续如需开放更多页面，请在"角色管理->分配权限"中操作
INSERT INTO sys_auth_role_permission (role_id, permission_id, created_by, created_time, is_deleted)
SELECT r.id, p.id, 'admin', NOW(), 0
FROM sys_auth_role r, sys_auth_permission p
WHERE r.role_key = 'common'
  AND p.permission_key = 'home:homeview'
  AND NOT EXISTS (SELECT 1 FROM sys_auth_role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id AND rp.is_deleted = 0);

COMMIT;

-- 执行完成后建议核对：
-- SELECT id,name,parent_id,type,permission_key,backend_url FROM sys_auth_permission ORDER BY parent_id,order_num;
-- SELECT * FROM sys_auth_role WHERE role_key='common';
