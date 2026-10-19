-- ============================================================================
-- V1.0.4__add_hot_query_indexes.sql
-- 目的：为热查询补齐索引，消除全表扫描。
--
-- 背景（本轮优化发现）：
--   1. sys_auth_role.role_key、sys_auth_permission.permission_key 上没有任何索引，
--      而二者是登录（按 role_key 判定超级角色）、注册（取默认角色）、
--      唯一性校验（countByRoleKey / countByKey）与会话构建
--      （selectByRoleKeys / selectByKeys）的主要查询条件；
--   2. 两张关联表只有以 user_id / role_id 为前导列的复合索引，
--      反向查询（按 role_id 查用户、按 permission_id 查角色）无法命中索引，
--      对应 countByRoleId（删除角色前的绑定校验）、selectUserIdsByRoleIds（缓存失效）、
--      selectByPermissionIds（删除权限时级联清理）等语句；
--   3. 实测（改动前 EXPLAIN）：上述四类查询均为 type=ALL、possible_keys=NULL；
--      改动后为 type=ref 且 key 命中新索引。
--
-- 说明：
--   - 唯一性仍由应用层校验（countByXxx 排除自身）保证，索引保持非唯一，
--     与既有 idx_user_name 的约定一致；这样逻辑删除后同名重建不会被唯一键挡住。
--   - 脚本幂等：先用 COUNT(*) 判断索引是否已存在，已存在则执行无害的 SELECT，可重复执行。
--   - 动态 SQL 中的提示文本使用纯 ASCII，避免客户端字符集差异影响解析。
--   - 仅新增索引，不改数据、不改列定义；回滚见文件末。
--
-- 执行方式（推荐用 source，避免通过管道传中文注释时出现解析异常）：
--   mysql --default-character-set=utf8mb4 -h<host> -P3306 -u<user> -p admintem_new ^
--         -e "source db/changelog/V1.0.4__add_hot_query_indexes.sql"
-- ============================================================================

SET NAMES utf8mb4;

-- [1/4] sys_auth_role.role_key
SELECT IF(COUNT(*) = 0,
          'ALTER TABLE sys_auth_role ADD INDEX idx_role_key (role_key)',
          'SELECT ''skip: idx_role_key exists'' AS msg')
INTO @ddl
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_auth_role' AND INDEX_NAME = 'idx_role_key';

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- [2/4] sys_auth_permission.permission_key
SELECT IF(COUNT(*) = 0,
          'ALTER TABLE sys_auth_permission ADD INDEX idx_permission_key (permission_key)',
          'SELECT ''skip: idx_permission_key exists'' AS msg')
INTO @ddl
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_auth_permission' AND INDEX_NAME = 'idx_permission_key';

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- [3/4] sys_auth_user_role.role_id  (reverse lookup: users of a role)
SELECT IF(COUNT(*) = 0,
          'ALTER TABLE sys_auth_user_role ADD INDEX idx_role_id (role_id)',
          'SELECT ''skip: idx_role_id exists'' AS msg')
INTO @ddl
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_auth_user_role' AND INDEX_NAME = 'idx_role_id';

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- [4/4] sys_auth_role_permission.permission_id  (reverse lookup: roles of a permission)
SELECT IF(COUNT(*) = 0,
          'ALTER TABLE sys_auth_role_permission ADD INDEX idx_permission_id (permission_id)',
          'SELECT ''skip: idx_permission_id exists'' AS msg')
INTO @ddl
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_auth_role_permission' AND INDEX_NAME = 'idx_permission_id';

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verify:
-- SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols
--   FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
--  GROUP BY TABLE_NAME, INDEX_NAME ORDER BY TABLE_NAME, INDEX_NAME;

-- Rollback:
-- ALTER TABLE sys_auth_role DROP INDEX idx_role_key;
-- ALTER TABLE sys_auth_permission DROP INDEX idx_permission_key;
-- ALTER TABLE sys_auth_user_role DROP INDEX idx_role_id;
-- ALTER TABLE sys_auth_role_permission DROP INDEX idx_permission_id;
