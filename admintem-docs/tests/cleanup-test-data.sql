-- ============================================================================
-- 测试脏数据物理清理（仅用于回归测试后恢复基线，勿在生产执行同款逻辑）
-- 删除范围：
--   1) 本次测试产生的 t_ 前缀用户/角色及其关联、空账号测试行；
--   2) 移动端 E2E（J 组 修改密码全流程）自建的 e2e_m_* 临时账号。
--      这些账号由接口按正常流程逻辑删除（is_deleted=1），业务上无影响，
--      但反复执行 E2E 会在表里留下一串"已删除"记录，物理清理可保持演示基线干净。
-- ============================================================================
SET NAMES utf8mb4;
BEGIN;

-- 校验待删除用户集合：t_ 前缀 / 空账号 / E2E 临时账号（均排除基线账号 1、3）
CREATE TEMPORARY TABLE tmp_del_user AS
SELECT id FROM sys_auth_user
WHERE (user_name LIKE 't\_%' OR user_name IS NULL OR user_name LIKE 'e2e\_m\_%')
  AND id NOT IN (1, 3);

CREATE TEMPORARY TABLE tmp_del_role AS
SELECT id FROM sys_auth_role
WHERE (role_name LIKE 't\_%' OR role_key LIKE 't\_%');

-- 删除关联（用户角色）
DELETE FROM sys_auth_user_role WHERE user_id IN (SELECT id FROM tmp_del_user);
-- 删除关联（角色权限）
DELETE FROM sys_auth_role_permission WHERE role_id IN (SELECT id FROM tmp_del_role);
-- 删除临时角色
DELETE FROM sys_auth_role WHERE id IN (SELECT id FROM tmp_del_role);
-- 删除临时用户
DELETE FROM sys_auth_user WHERE id IN (SELECT id FROM tmp_del_user);
-- 删除测试期间创建的临时权限（含逻辑删除残留；permission_key 统一为 sysAuthUser:t_demo_ 前缀）
DELETE FROM sys_auth_permission WHERE permission_key LIKE 'sysAuthUser:t\_demo\_%';

-- 菜单字段兜底归一：回归测试第 14 组会临时改动「角色管理」菜单的 status/show 以验证两个字段
-- 是否生效；若脚本被中断可能留下脏值。此处按基线口径统一归一为 启用(0) + 展示(0)。
-- 注意：这是"恢复基线"操作，会取消管理员有意设置的隐藏/禁用菜单，仅在测试环境使用。
UPDATE sys_auth_permission
SET status = 0, `show` = 0, update_by = 'cleanup', update_time = NOW()
WHERE type = 0 AND is_deleted = 0
  AND (status IS NULL OR status <> 0 OR `show` IS NULL OR `show` <> 0);

DROP TEMPORARY TABLE tmp_del_user;
DROP TEMPORARY TABLE tmp_del_role;

COMMIT;
