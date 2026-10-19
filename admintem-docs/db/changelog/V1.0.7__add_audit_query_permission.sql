-- ============================================================================
-- V1.0.7：新增「操作审计」菜单与查询权限
--
-- 背景：审计表（V1.0.5）一直只写不读，运维要查只能连库跑 SQL；
--       本次补上查询接口与页面，因此需要两条权限数据：
--         1) 菜单（type=0）：/auditLog → 组件 auditLog/index，权限标识 auditLog:auditLog
--         2) 按钮（type=1）：sysAuthAuditLog:paginQuery（分页查询 + 动作字典共用）
--
-- 幂等性：全部用 INSERT ... SELECT ... WHERE NOT EXISTS 守卫，
--         重复执行不会产生重复行（Flyway 重放或手工执行都安全）。
-- 授权：把两条权限授予内置管理员角色（role_id=1）。
--       admin 在代码里对 @PreAuthorize 有旁路，但**菜单可见性仍依赖授权**，
--       所以这里必须写关联，否则管理员登录后看不到新菜单。
--
-- 回滚（如需）：
--   DELETE FROM sys_auth_role_permission WHERE permission_id IN
--     (SELECT id FROM sys_auth_permission WHERE permission_key IN ('auditLog:auditLog','sysAuthAuditLog:paginQuery'));
--   DELETE FROM sys_auth_permission WHERE permission_key IN ('auditLog:auditLog','sysAuthAuditLog:paginQuery');
--   （生产建议用逻辑删除：UPDATE ... SET is_deleted=1，保留审计链）
-- ============================================================================

USE `admintem_new`;

-- 1) 菜单：挂在「系统管理」（parent_id=2）下，排第 4 位
INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url,
     status, `show`, icon, permission_key, created_by, created_time, is_deleted)
SELECT '操作审计', 2, 4, 0, '/auditLog', 'auditLog/index', NULL,
       0, 0, NULL, 'auditLog:auditLog', 'system', NOW(), 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_auth_permission
    WHERE permission_key = 'auditLog:auditLog' AND is_deleted = 0
);

-- 2) 按钮：父节点为上一步的菜单
SET @audit_menu_id = (
    SELECT id FROM sys_auth_permission
    WHERE permission_key = 'auditLog:auditLog' AND is_deleted = 0
    LIMIT 1
);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url,
     status, `show`, icon, permission_key, created_by, created_time, is_deleted)
SELECT '审计查询', @audit_menu_id, 1, 1, NULL, NULL, NULL,
       0, NULL, NULL, 'sysAuthAuditLog:paginQuery', 'system', NOW(), 0
FROM DUAL
WHERE @audit_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_auth_permission
    WHERE permission_key = 'sysAuthAuditLog:paginQuery' AND is_deleted = 0
  );

-- 3) 授予内置管理员角色（role_id=1）
INSERT INTO sys_auth_role_permission (role_id, permission_id, created_by, created_time, is_deleted)
SELECT 1, p.id, 'system', NOW(), 0
FROM sys_auth_permission p
WHERE p.is_deleted = 0
  AND p.permission_key IN ('auditLog:auditLog', 'sysAuthAuditLog:paginQuery')
  AND NOT EXISTS (
    SELECT 1 FROM sys_auth_role_permission rp
    WHERE rp.role_id = 1 AND rp.permission_id = p.id AND rp.is_deleted = 0
  );

-- 4) 自检输出（执行后在客户端可见，便于确认结果）
SELECT id, name, parent_id, type, permission_key
FROM sys_auth_permission
WHERE permission_key IN ('auditLog:auditLog', 'sysAuthAuditLog:paginQuery') AND is_deleted = 0;
