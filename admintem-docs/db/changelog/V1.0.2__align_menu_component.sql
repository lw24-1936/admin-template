-- ============================================================================
-- admintem-backend 数据库增量变更脚本  V1.0.2
-- ----------------------------------------------------------------------------
-- 目标库   : admintem_new
-- 变更目的 : 对齐“前端完全动态路由”约定
--   1. sys_auth_permission.component 统一为 src/views 下的相对路径（不含扩展名、
--      不含前导斜杠），前端据此用 import.meta.glob 自动映射组件并动态注册路由；
--   2. 仅更新已有菜单记录的 component 值（幂等：component 已为目标值则跳过），
--      不改动任何其它字段与数据；
--   3. 约定示例：工作台 -> HomeView（views/HomeView.vue）；
--      用户管理 -> userInfo/UserInfo（views/userInfo/UserInfo.vue）。
-- 执行方式 :
--   mysql -h<host> -P3306 -uroot -p admintem_new < V1.0.2__align_menu_component.sql
-- 对应文档 : 02-系统设计文档（动态路由）、05-接口与权限标识对照表
-- ============================================================================

SET NAMES utf8mb4;
BEGIN;

-- 1) 工作台（顶级菜单，组件位于 views/HomeView.vue）
UPDATE sys_auth_permission
SET component = 'HomeView', update_time = NOW()
WHERE permission_key = 'home:homeview'
  AND (component IS NULL OR component <> 'HomeView')
  AND is_deleted = 0;

-- 2) 用户管理（组件位于 views/userInfo/UserInfo.vue）
UPDATE sys_auth_permission
SET component = 'userInfo/UserInfo', update_time = NOW()
WHERE permission_key = 'userInfo:userInfo'
  AND (component IS NULL OR component <> 'userInfo/UserInfo')
  AND is_deleted = 0;

-- 3) 角色管理（组件位于 views/userInfo/RoleInfo.vue）
UPDATE sys_auth_permission
SET component = 'userInfo/RoleInfo', update_time = NOW()
WHERE permission_key = 'userInfo:roleInfo'
  AND (component IS NULL OR component <> 'userInfo/RoleInfo')
  AND is_deleted = 0;

-- 4) 权限管理（组件位于 views/userInfo/PermissionInfo.vue）
UPDATE sys_auth_permission
SET component = 'userInfo/PermissionInfo', update_time = NOW()
WHERE permission_key = 'userInfo:permissionInfo'
  AND (component IS NULL OR component <> 'userInfo/PermissionInfo')
  AND is_deleted = 0;

-- 5) 系统管理为纯分组菜单（无页面组件），确保其 component 为空字符串
UPDATE sys_auth_permission
SET component = '', update_time = NOW()
WHERE permission_key = 'system:management'
  AND component IS NOT NULL
  AND component <> ''
  AND is_deleted = 0;

COMMIT;

-- 核对：SELECT id,name,menu_url,component,permission_key FROM sys_auth_permission WHERE type=0 AND is_deleted=0;
