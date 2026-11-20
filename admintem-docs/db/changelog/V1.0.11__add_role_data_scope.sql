-- ============================================================================
-- V1.0.11 角色数据范围（行级数据权限）
--
-- 值说明：
--   ALL             全部数据（默认，兼容历史行为）
--   DEPT            本部门
--   DEPT_AND_CHILD  本部门及所有子部门
--   SELF            仅本人
--
-- 注意：全量初始化脚本已包含 data_scope 列，Flyway 在全新库上也会执行本脚本，
-- 因此这里必须幂等（column 已存在时跳过 ALTER），否则 CI 的"初始化 + Flyway"会失败。
-- ============================================================================

SET @data_scope_column_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_auth_role'
      AND COLUMN_NAME = 'data_scope'
);

SET @data_scope_ddl = IF(
    @data_scope_column_exists = 0,
    'ALTER TABLE `sys_auth_role` ADD COLUMN `data_scope` varchar(20) NOT NULL DEFAULT ''ALL'' COMMENT ''数据范围：ALL/DEPT/DEPT_AND_CHILD/SELF'' AFTER `role_key`',
    'SELECT 1'
);

PREPARE data_scope_stmt FROM @data_scope_ddl;
EXECUTE data_scope_stmt;
DEALLOCATE PREPARE data_scope_stmt;

UPDATE `sys_auth_role`
SET `data_scope` = 'ALL'
WHERE `data_scope` IS NULL OR `data_scope` = '';
