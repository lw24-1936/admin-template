-- ============================================================================
-- V1.0.6__add_active_unique_indexes.sql
--
-- 目的：把"账号 / 角色标识 / 权限标识"的唯一性从"仅应用层校验"变成"数据库兜底"。
--
-- 背景（代码审查发现）：
--   1. 现状：sys_auth_user.user_name、sys_auth_role.role_key、sys_auth_permission.permission_key
--      都只有普通索引（NON_UNIQUE=1），唯一性靠 Service 里"先 count 再 insert"保证；
--   2. 风险：并发下两个请求可同时通过 count 校验，随后都插入成功 —— 表里出现同名账号。
--      登录查询 selectByUserName 走 LIMIT 1，身份会随机落到其中一行，权限随之错乱；
--   3. 为什么不用普通唯一索引：本系统采用逻辑删除（is_deleted），删除后需要能用同名重建，
--      普通唯一索引会把"历史删除行"也算进去，导致重建失败。
--
-- 方案：MySQL 8.0.13+ 的**函数索引**，只对未删除行建立唯一约束：
--       唯一键 = IF(is_deleted = 0, 业务键, NULL)
--       逻辑删除后表达式结果为 NULL，MySQL 唯一索引允许多个 NULL，因此可同名重建；
--       未删除行之间则被强制唯一。
--
-- 说明：
--   - 三个索引相互独立，任一已存在则跳过（脚本幂等，可重复执行）；
--   - 执行前会输出一次重复数据检查（正常应为空；若有重复需先人工合并，
--     否则 ALTER 会失败并提示 Duplicate entry，这是期望行为——宁可失败也不要静默放过）；
--   - 应用层仍保留 count 校验用于返回友好提示，数据库约束负责兜住并发；
--     并发触发的 DuplicateKeyException 会被服务层转换为 505/506 类业务码（见 SysAuthXxxServiceImpl）。
--
-- 执行方式（推荐 source，避免管道传中文注释时解析异常）：
--   mysql --default-character-set=utf8mb4 -h<host> -P3306 -u<user> -p admintem_new ^
--         -e "source db/changelog/V1.0.6__add_active_unique_indexes.sql"
--
-- 回滚：
--   ALTER TABLE sys_auth_user       DROP INDEX uk_user_name_active;
--   ALTER TABLE sys_auth_role       DROP INDEX uk_role_key_active;
--   ALTER TABLE sys_auth_permission DROP INDEX uk_permission_key_active;
--
-- 对应文档：03-数据库设计与变更说明（变更清单 V1.0.6）、09-代码审计与质量记录（审查记录）
-- ============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- [0/3] 前置检查：输出重复数据（正常应为空结果集）
-- ---------------------------------------------------------------------------
SELECT 'dup_user_name' AS check_item, user_name AS business_key, COUNT(*) AS duplicate_rows
FROM sys_auth_user WHERE is_deleted = 0 GROUP BY user_name HAVING COUNT(*) > 1;

SELECT 'dup_role_key' AS check_item, role_key AS business_key, COUNT(*) AS duplicate_rows
FROM sys_auth_role WHERE is_deleted = 0 GROUP BY role_key HAVING COUNT(*) > 1;

SELECT 'dup_permission_key' AS check_item, permission_key AS business_key, COUNT(*) AS duplicate_rows
FROM sys_auth_permission WHERE is_deleted = 0 GROUP BY permission_key HAVING COUNT(*) > 1;

-- ---------------------------------------------------------------------------
-- [1/3] sys_auth_user.user_name —— 未删除行唯一
-- ---------------------------------------------------------------------------
SELECT IF(COUNT(*) = 0,
          'ALTER TABLE sys_auth_user ADD UNIQUE KEY uk_user_name_active ((IF(is_deleted = 0, user_name, NULL)))',
          'SELECT ''skip: uk_user_name_active exists'' AS msg')
INTO @ddl
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_auth_user' AND INDEX_NAME = 'uk_user_name_active';

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- [2/3] sys_auth_role.role_key —— 未删除行唯一
-- ---------------------------------------------------------------------------
SELECT IF(COUNT(*) = 0,
          'ALTER TABLE sys_auth_role ADD UNIQUE KEY uk_role_key_active ((IF(is_deleted = 0, role_key, NULL)))',
          'SELECT ''skip: uk_role_key_active exists'' AS msg')
INTO @ddl
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_auth_role' AND INDEX_NAME = 'uk_role_key_active';

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- [3/3] sys_auth_permission.permission_key —— 未删除行唯一
-- ---------------------------------------------------------------------------
SELECT IF(COUNT(*) = 0,
          'ALTER TABLE sys_auth_permission ADD UNIQUE KEY uk_permission_key_active ((IF(is_deleted = 0, permission_key, NULL)))',
          'SELECT ''skip: uk_permission_key_active exists'' AS msg')
INTO @ddl
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_auth_permission' AND INDEX_NAME = 'uk_permission_key_active';

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- [校验] 三个唯一索引应全部存在且 NON_UNIQUE = 0
-- ---------------------------------------------------------------------------
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, EXPRESSION
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME IN ('uk_user_name_active', 'uk_role_key_active', 'uk_permission_key_active')
ORDER BY TABLE_NAME;
