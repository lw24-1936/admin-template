-- ============================================================================
-- V1.0.9：部门表、用户挂部门、部门菜单与权限
--
-- 背景：现有 5 张 RBAC 表只解决"功能权限"（能看哪些菜单、能点哪些按钮），
--       没有组织结构。用户无法归属部门，列表也无法按部门展示。
--       本次加入最小可用的组织维度：树形部门 + 用户归属单个部门。
--
-- 范围与边界（刻意不做）：
--   - 不做"数据权限"（按部门过滤业务数据），那是 RBAC → ABAC 的跨越，单独立项；
--   - 一个用户只属于一个部门（dept_id 单值），不做多部门兼职。
--
-- 内容：
--   1) 建表 sys_dept
--   2) sys_auth_user 增加 dept_id 与索引
--   3) 菜单「部门管理」+ 三个按钮（新增/编辑/删除）并授予管理员
--   4) 演示数据：总公司 / 技术部 / 产品部；admin 归总公司，test 归技术部
--
-- 幂等：建表 IF NOT EXISTS；列存在性用 information_schema 判断；
--       权限与演示数据用 INSERT ... SELECT ... WHERE NOT EXISTS。
-- 回滚（按顺序）：
--   DELETE FROM sys_auth_role_permission WHERE permission_id IN
--     (SELECT id FROM sys_auth_permission WHERE permission_key LIKE 'sysAuthDept:%'
--        OR permission_key = 'dept:dept');
--   DELETE FROM sys_auth_permission WHERE permission_key LIKE 'sysAuthDept:%'
--     OR permission_key = 'dept:dept';
--   ALTER TABLE sys_auth_user DROP FOREIGN KEY fk_user_dept;  -- 若有
--   ALTER TABLE sys_auth_user DROP INDEX idx_user_dept, DROP COLUMN dept_id;
--   DROP TABLE IF EXISTS sys_dept;
-- ============================================================================

USE `admintem_new`;

-- 1) 部门表
CREATE TABLE IF NOT EXISTS `sys_dept` (
  `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id`   bigint   NOT NULL DEFAULT 0 COMMENT '上级部门ID，0 表示顶级',
  `dept_name`   varchar(64) NOT NULL COMMENT '部门名称',
  `order_num`   int      NOT NULL DEFAULT 1 COMMENT '同级排序号（升序）',
  `leader`      varchar(32)  DEFAULT NULL COMMENT '负责人',
  `phone`       varchar(20)  DEFAULT NULL COMMENT '联系电话',
  `status`      int      NOT NULL DEFAULT 0 COMMENT '状态：0 启用 / 1 禁用',
  `created_by`  varchar(32)  DEFAULT NULL COMMENT '创建人',
  `created_time` datetime    DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(32)  DEFAULT NULL COMMENT '更新人',
  `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
  `is_deleted`  int      NOT NULL DEFAULT 0 COMMENT '是否删除：0 否 / 1 是',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_dept_parent` (`parent_id`),
  -- 未删除行部门名唯一（函数索引：已删除行求值为 NULL，可同名重建）
  UNIQUE KEY `uk_dept_name_active` ((IF(`is_deleted` = 0, `dept_name`, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '部门表';

-- 2) 用户表挂部门（列不存在才加，避免重复执行报错）
SET @has_dept_col = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_auth_user' AND column_name = 'dept_id'
);
SET @ddl = IF(@has_dept_col = 0,
    'ALTER TABLE `sys_auth_user` ADD COLUMN `dept_id` bigint DEFAULT NULL COMMENT ''所属部门ID（V1.0.9）'' AFTER `sex`, ADD KEY `idx_user_dept` (`dept_id`)',
    'SELECT ''sys_auth_user.dept_id 已存在，跳过'' AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 菜单 + 按钮
INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url,
     status, `show`, icon, permission_key, created_by, created_time, is_deleted)
SELECT '部门管理', 2, 6, 0, '/dept', 'dept/index', NULL,
       0, 0, NULL, 'dept:dept', 'system', NOW(), 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_auth_permission WHERE permission_key = 'dept:dept' AND is_deleted = 0
);

SET @dept_menu_id = (SELECT id FROM sys_auth_permission WHERE permission_key = 'dept:dept' AND is_deleted = 0 LIMIT 1);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url,
     status, `show`, icon, permission_key, created_by, created_time, is_deleted)
SELECT '新增部门', @dept_menu_id, 1, 1, NULL, NULL, '/sysAuthDept/addDept',
       0, NULL, NULL, 'sysAuthDept:addDept', 'system', NOW(), 0
FROM DUAL
WHERE @dept_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_auth_permission WHERE permission_key = 'sysAuthDept:addDept' AND is_deleted = 0);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url,
     status, `show`, icon, permission_key, created_by, created_time, is_deleted)
SELECT '编辑部门', @dept_menu_id, 2, 1, NULL, NULL, '/sysAuthDept/editDept',
       0, NULL, NULL, 'sysAuthDept:editDept', 'system', NOW(), 0
FROM DUAL
WHERE @dept_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_auth_permission WHERE permission_key = 'sysAuthDept:editDept' AND is_deleted = 0);

INSERT INTO sys_auth_permission
    (name, parent_id, order_num, type, menu_url, component, backend_url,
     status, `show`, icon, permission_key, created_by, created_time, is_deleted)
SELECT '删除部门', @dept_menu_id, 3, 1, NULL, NULL, '/sysAuthDept/deleteById',
       0, NULL, NULL, 'sysAuthDept:deleteById', 'system', NOW(), 0
FROM DUAL
WHERE @dept_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_auth_permission WHERE permission_key = 'sysAuthDept:deleteById' AND is_deleted = 0);

-- 授予内置管理员
INSERT INTO sys_auth_role_permission (role_id, permission_id, created_by, created_time, is_deleted)
SELECT 1, p.id, 'system', NOW(), 0
FROM sys_auth_permission p
WHERE p.is_deleted = 0
  AND (p.permission_key = 'dept:dept' OR p.permission_key LIKE 'sysAuthDept:%')
  AND NOT EXISTS (
      SELECT 1 FROM sys_auth_role_permission rp
      WHERE rp.role_id = 1 AND rp.permission_id = p.id AND rp.is_deleted = 0
  );

-- 4) 演示数据（幂等：按名称判断）
INSERT INTO `sys_dept` (`parent_id`,`dept_name`,`order_num`,`leader`,`phone`,`status`,`created_by`,`created_time`,`is_deleted`)
SELECT 0, '总公司', 1, '总经理', '010-00000001', 0, 'system', NOW(), 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_name = '总公司' AND is_deleted = 0);

INSERT INTO `sys_dept` (`parent_id`,`dept_name`,`order_num`,`leader`,`phone`,`status`,`created_by`,`created_time`,`is_deleted`)
SELECT (SELECT id FROM sys_dept WHERE dept_name = '总公司' AND is_deleted = 0 LIMIT 1),
       '技术部', 1, '技术负责人', '010-00000002', 0, 'system', NOW(), 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_name = '技术部' AND is_deleted = 0);

INSERT INTO `sys_dept` (`parent_id`,`dept_name`,`order_num`,`leader`,`phone`,`status`,`created_by`,`created_time`,`is_deleted`)
SELECT (SELECT id FROM sys_dept WHERE dept_name = '总公司' AND is_deleted = 0 LIMIT 1),
       '产品部', 2, '产品负责人', '010-00000003', 0, 'system', NOW(), 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_name = '产品部' AND is_deleted = 0);

-- 演示归属：admin -> 总公司；test -> 技术部（仅在未分配时写入，不覆盖真实数据）
UPDATE sys_auth_user
SET dept_id = (SELECT id FROM sys_dept WHERE dept_name = '总公司' AND is_deleted = 0 LIMIT 1)
WHERE user_name = 'admin' AND dept_id IS NULL;

UPDATE sys_auth_user
SET dept_id = (SELECT id FROM sys_dept WHERE dept_name = '技术部' AND is_deleted = 0 LIMIT 1)
WHERE user_name = 'test' AND dept_id IS NULL;

-- 5) 自检
SELECT id, parent_id, dept_name, order_num, status FROM sys_dept WHERE is_deleted = 0 ORDER BY parent_id, order_num;
SELECT user_name, dept_id FROM sys_auth_user WHERE is_deleted = 0;
SELECT id, name, permission_key FROM sys_auth_permission
WHERE permission_key IN ('dept:dept','sysAuthDept:addDept','sysAuthDept:editDept','sysAuthDept:deleteById')
  AND is_deleted = 0;
