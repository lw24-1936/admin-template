-- ============================================================================
-- V1.0.8：新增「在线会话」菜单与强制下线权限
--
-- 背景：令牌白名单（Redis admintem:auth:jwt:*）本身就是在线表，但此前没有界面，
--       运维要踢人只能连 Redis 删 key。本次补上查看与强制下线能力。
--
-- 两条权限：
--   1) 菜单（type=0）：/onlineSession → 组件 onlineSession/index，
--      permission_key = onlineSession:onlineSession
--   2) 按钮（type=1）：sysAuthSession:kick（强制下线）
--
-- 幂等：INSERT ... SELECT ... WHERE NOT EXISTS；
-- 授权：授予内置管理员角色（role_id=1）——菜单可见性依赖授权行。
--
-- 回滚：
--   DELETE FROM sys_auth_role_permission WHERE permission_id IN
--     (SELECT id FROM sys_auth_permission WHERE permission_key IN
--        ('onlineSession:onlineSession','sysAuthSession:kick'));
--   DELETE FROM sys_auth_permission WHERE permission_key IN
--     ('onlineSession:onlineSession','sysAuthSession:kick');
-- ============================================================================

USE `admintem_new`;

-- 1) 菜单：挂在「系统管理」下，排第 5 位（操作审计之后）
INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url,
     status, `show`, icon, permission_key, created_by, created_time, is_deleted)
SELECT '在线会话', 2, 5, 0, '/onlineSession', 'onlineSession/index', NULL,
       0, 0, NULL, 'onlineSession:onlineSession', 'system', NOW(), 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_auth_permission
    WHERE permission_key = 'onlineSession:onlineSession' AND is_deleted = 0
);

-- 2) 按钮：父节点为上一步的菜单
SET @session_menu_id = (
    SELECT id FROM sys_auth_permission
    WHERE permission_key = 'onlineSession:onlineSession' AND is_deleted = 0
    LIMIT 1
);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url,
     status, `show`, icon, permission_key, created_by, created_time, is_deleted)
SELECT '强制下线', @session_menu_id, 1, 1, NULL, NULL, '/sysAuthSession/kick',
       0, NULL, NULL, 'sysAuthSession:kick', 'system', NOW(), 0
FROM DUAL
WHERE @session_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_auth_permission
    WHERE permission_key = 'sysAuthSession:kick' AND is_deleted = 0
  );

-- 3) 授予内置管理员角色
INSERT INTO sys_auth_role_permission (role_id, permission_id, created_by, created_time, is_deleted)
SELECT 1, p.id, 'system', NOW(), 0
FROM sys_auth_permission p
WHERE p.is_deleted = 0
  AND p.permission_key IN ('onlineSession:onlineSession', 'sysAuthSession:kick')
  AND NOT EXISTS (
    SELECT 1 FROM sys_auth_role_permission rp
    WHERE rp.role_id = 1 AND rp.permission_id = p.id AND rp.is_deleted = 0
  );

-- 4) 自检
SELECT id, name, parent_id, type, permission_key
FROM sys_auth_permission
WHERE permission_key IN ('onlineSession:onlineSession', 'sysAuthSession:kick') AND is_deleted = 0;
