-- ============================================================================
-- admintem_backend 一键全量初始化脚本（00_admintem_full_init.sql）
-- ----------------------------------------------------------------------------
-- 适用场景：在「新建/测试/演示」库上一次性重建 RBAC 五张业务表 + 审计日志表，
--           并灌入基础数据，便于换环境或演示前快速恢复到与线上一致的基线。
--
-- 【执行前检查（务必阅读）】
--   1. 本脚本会 DROP 并重建以下 6 张表，将覆盖其现有数据：
--        sys_auth_permission / sys_auth_role / sys_auth_role_permission
--        sys_auth_user / sys_auth_user_role / sys_auth_audit_log
--      （审计日志表按要求一并清空：本脚本的语义是"重置到演示基线"，
--        如需保留审计历史请勿在生产执行本脚本，或先单独导出该表）
--   2. 其它业务表（sz_* 等，如有）不在处理范围，不会受影响；
--   3. 请先确认当前数据库为可重建的测试/演示库；若在共享/生产库执行，
--      必须先导出备份（见下"备份建议"）；
--   4. 目标库需预先存在（本脚本默认使用 admintem_new；如需从零建库，
--      取消文件末尾"可选：整库初始化"注释段并去掉下方注释即可）；
--   5. 脚本整体幂等：可重复执行（每次都会按种子重置基线）。
--
-- 备份建议：
--   mysqldump -h<host> -P3306 -uroot -p admintem_new > admintem_new_backup_YYYYMMDD.sql
--
-- 执行方式（**破坏性脚本，需显式确认**）：
--   mysql -h<host> -P3306 -uroot -p --default-character-set=utf8mb4 admintem_new
--   mysql> SET @admintem_confirm_reset = 1;      -- 没有这一行会直接中止，不会删任何表
--   mysql> source db/init/00_admintem_full_init.sql
--
-- 对应文档：03-数据库设计与变更说明（一键初始化章节）
-- 生成基准：与线上 admintem_new 基线一致（用户 2 / 角色 3 / 权限 26，含 V1.0.7 审计、V1.0.8 在线会话与 V1.0.9 部门）
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 目标库（脚本从此处起操作该库；如需整库重建见下方注释）
USE `admintem_new`;

-- ----------------------------------------------------------------------------
-- 安全闸门（V1.0.6 加入）
--
-- 为什么需要：本脚本会 DROP 并重建全部表。历史上曾因"拿它去校验 DDL 语法"
-- 而误清空过线上库的审计表（当时脚本里只有一句静默的 USE `admintem_new`），
-- 说明"危险动作 + 无声默认"是必须消除的组合。
-- 现在未显式执行 `SET @admintem_confirm_reset = 1` 时直接 SIGNAL 中止，
-- 表不会被删除，也不会残留半成品结构。
-- 注意：必须先 USE 再建存储过程，否则客户端未选库时会报 No database selected。
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `admintem_guard_reset`;
DELIMITER $$
CREATE PROCEDURE `admintem_guard_reset`()
BEGIN
    IF IFNULL(@admintem_confirm_reset, 0) <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '拒绝执行：本脚本会删除并重建全部表；请先执行 SET @admintem_confirm_reset = 1 再运行';
    END IF;
END$$
DELIMITER ;
CALL `admintem_guard_reset`();
DROP PROCEDURE `admintem_guard_reset`;

-- 若希望"整库级"初始化（DROP 整个库后重建），可手工执行下列三行：
--   DROP DATABASE IF EXISTS `admintem_new`;
--   CREATE DATABASE `admintem_new` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
--   USE `admintem_new`;
-- 注意：DROP DATABASE 会删除该库下所有表，请在确认无共享数据后使用。

-- ============================================================================
-- 一、重建五张 RBAC 表
-- ============================================================================

-- 1) 权限表（菜单 type=0 / 按钮 type=1；`show` 为保留字需反引号）
DROP TABLE IF EXISTS `sys_auth_permission`;
CREATE TABLE `sys_auth_permission` (
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `name`           varchar(64)  DEFAULT NULL COMMENT '权限名称',
    `parent_id`      bigint       DEFAULT NULL COMMENT '父菜单ID(0=顶级)',
    `order_num`      int          DEFAULT NULL COMMENT '显示顺序',
    `type`           tinyint      DEFAULT NULL COMMENT '权限类型 0菜单 1按钮',
    `menu_url`       varchar(255) DEFAULT NULL COMMENT '菜单路由地址(前端)',
    `component`      varchar(255) DEFAULT NULL COMMENT '组件路径(views相对路径,动态路由)',
    `backend_url`    varchar(255) DEFAULT NULL COMMENT '后端接口地址(审计/展示)',
    `status`         tinyint      DEFAULT NULL COMMENT '状态 0启用 1禁用(null按启用)',
    `show`           tinyint      DEFAULT NULL COMMENT '展示状态 0展示 1隐藏',
    `icon`           varchar(128) DEFAULT NULL COMMENT '图标标识',
    `permission_key` varchar(64)  DEFAULT NULL COMMENT '权限唯一标识(前后端共用)',
    `created_by`     varchar(32)  DEFAULT NULL COMMENT '创建人',
    `created_time`   datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`      varchar(32)  DEFAULT NULL COMMENT '更新人',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    `is_deleted`     int          NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
    PRIMARY KEY (`id`) USING BTREE,
    -- permission_key 是唯一性校验与会话构建（selectByKeys）的查询条件（V1.0.4 补索引）
    KEY `idx_permission_key` (`permission_key`),
    -- 仅对未删除行唯一（V1.0.6 函数索引）：并发写同名 permission_key 由数据库兜底；
    -- 逻辑删除后表达式为 NULL，MySQL 唯一索引允许多个 NULL，因此可同名重建
    UNIQUE KEY `uk_permission_key_active` ((IF(`is_deleted` = 0, `permission_key`, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '权限表(菜单+按钮)' ROW_FORMAT = DYNAMIC;

-- 2) 角色表
DROP TABLE IF EXISTS `sys_auth_role`;
CREATE TABLE `sys_auth_role` (
    `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name`    varchar(32) DEFAULT NULL COMMENT '角色名称',
    `role_key`     varchar(64) DEFAULT NULL COMMENT '角色唯一标识(admin=超级)',
    `data_scope`   varchar(20) NOT NULL DEFAULT 'ALL' COMMENT '数据范围：ALL/DEPT/DEPT_AND_CHILD/SELF',
    `created_by`   varchar(32) DEFAULT NULL COMMENT '创建人',
    `created_time` datetime    DEFAULT NULL COMMENT '创建时间',
    `update_by`    varchar(32) DEFAULT NULL COMMENT '更新人',
    `update_time`  datetime    DEFAULT NULL COMMENT '更新时间',
    `is_deleted`   int         NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
    PRIMARY KEY (`id`) USING BTREE,
    -- role_key 是登录判定超级角色、注册取默认角色与唯一性校验的查询条件（V1.0.4 补索引）
    KEY `idx_role_key` (`role_key`),
    -- 仅对未删除行唯一（V1.0.6 函数索引），说明同 permission_key
    UNIQUE KEY `uk_role_key_active` ((IF(`is_deleted` = 0, `role_key`, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '角色表' ROW_FORMAT = DYNAMIC;

-- 3) 角色-权限关联表
DROP TABLE IF EXISTS `sys_auth_role_permission`;
CREATE TABLE `sys_auth_role_permission` (
    `id`            bigint   NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `role_id`       bigint   DEFAULT NULL COMMENT '角色ID',
    `permission_id` bigint   DEFAULT NULL COMMENT '权限ID',
    `created_by`    varchar(32) DEFAULT NULL COMMENT '创建人',
    `created_time`  datetime DEFAULT NULL COMMENT '创建时间',
    `update_by`     varchar(32) DEFAULT NULL COMMENT '更新人',
    `update_time`   datetime DEFAULT NULL COMMENT '更新时间',
    `is_deleted`    int      NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_role_permission` (`role_id`, `permission_id`),
    -- 反向查询：按权限查角色（删除权限时级联清理关联，V1.0.4 补索引）
    KEY `idx_permission_id` (`permission_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '角色权限关联表' ROW_FORMAT = DYNAMIC;

-- 4) 用户表
DROP TABLE IF EXISTS `sys_auth_user`;
CREATE TABLE `sys_auth_user` (
    `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_name`    varchar(32) DEFAULT NULL COMMENT '登录账号',
    `nick_name`    varchar(32) DEFAULT NULL COMMENT '昵称',
    `email`        varchar(32) DEFAULT NULL COMMENT '邮箱',
    `phone`        varchar(32) DEFAULT NULL COMMENT '手机号',
    `password`     varchar(64) DEFAULT NULL COMMENT '密码(BCrypt)',
    `sex`          tinyint     DEFAULT NULL COMMENT '性别 0女 1男',
    `avatar`       varchar(255) DEFAULT NULL COMMENT '头像',
    `dept_id`      bigint      DEFAULT NULL COMMENT '所属部门ID（V1.0.9）',
    `status`       tinyint     DEFAULT NULL COMMENT '状态 0启用 1禁用',
    `introduce`    varchar(255) DEFAULT NULL COMMENT '个人介绍',
    `ext_json`     varchar(255) DEFAULT NULL COMMENT '扩展字段',
    `created_by`   varchar(32) DEFAULT NULL COMMENT '创建人',
    `created_time` datetime    DEFAULT NULL COMMENT '创建时间',
    `update_by`    varchar(32) DEFAULT NULL COMMENT '更新人',
    `update_time`  datetime    DEFAULT NULL COMMENT '更新时间',
    `is_deleted`   int         NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_name` (`user_name`),
    KEY `idx_user_dept` (`dept_id`),
    -- 仅对未删除行唯一（V1.0.6 函数索引），说明同 permission_key
    UNIQUE KEY `uk_user_name_active` ((IF(`is_deleted` = 0, `user_name`, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- 4.1) 部门表（V1.0.9）
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
    `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    `parent_id`    bigint      NOT NULL DEFAULT 0 COMMENT '上级部门ID，0 表示顶级',
    `dept_name`    varchar(64) NOT NULL COMMENT '部门名称',
    `order_num`    int         NOT NULL DEFAULT 1 COMMENT '同级排序号（升序）',
    `leader`       varchar(32) DEFAULT NULL COMMENT '负责人',
    `phone`        varchar(20) DEFAULT NULL COMMENT '联系电话',
    `status`       int         NOT NULL DEFAULT 0 COMMENT '状态：0 启用 / 1 禁用',
    `created_by`   varchar(32) DEFAULT NULL COMMENT '创建人',
    `created_time` datetime    DEFAULT NULL COMMENT '创建时间',
    `update_by`    varchar(32) DEFAULT NULL COMMENT '更新人',
    `update_time`  datetime    DEFAULT NULL COMMENT '更新时间',
    `is_deleted`   int         NOT NULL DEFAULT 0 COMMENT '是否删除：0 否 / 1 是',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_dept_parent` (`parent_id`),
    -- 未删除行部门名唯一（函数索引，同 uk_user_name_active）
    UNIQUE KEY `uk_dept_name_active` ((IF(`is_deleted` = 0, `dept_name`, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '部门表' ROW_FORMAT = DYNAMIC;

-- 5) 用户-角色关联表
DROP TABLE IF EXISTS `sys_auth_user_role`;
CREATE TABLE `sys_auth_user_role` (
    `id`           bigint   NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `user_id`      bigint   DEFAULT NULL COMMENT '用户ID',
    `role_id`      bigint   DEFAULT NULL COMMENT '角色ID',
    `created_by`   varchar(32) DEFAULT NULL COMMENT '创建人',
    `created_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by`    varchar(32) DEFAULT NULL COMMENT '更新人',
    `update_time`  datetime DEFAULT NULL COMMENT '更新时间',
    `is_deleted`   int      NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_role` (`user_id`, `role_id`),
    -- 反向查询：按角色查用户（删除角色前的绑定校验、权限变更后的缓存失效，V1.0.4 补索引）
    KEY `idx_role_id` (`role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '用户角色关联表' ROW_FORMAT = DYNAMIC;

-- 6) 操作审计日志表（V1.0.5 新增；只追加，无 is_deleted）
--    结构变更请同步 db/changelog/V1.0.5__add_audit_log.sql
DROP TABLE IF EXISTS `sys_auth_audit_log`;
CREATE TABLE `sys_auth_audit_log` (
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `trace_id`     varchar(64)  DEFAULT NULL COMMENT '链路ID（与请求日志/SQL日志共用）',
    `operator`     varchar(32)  DEFAULT NULL COMMENT '操作人账号（登录类动作记录被尝试的账号）',
    `operator_id`  bigint       DEFAULT NULL COMMENT '操作人用户ID（匿名请求为 NULL）',
    `action`       varchar(64)  NOT NULL COMMENT '动作码，如 LOGIN / USER_ADD',
    `action_name`  varchar(64)  DEFAULT NULL COMMENT '动作中文名',
    `target_type`  varchar(32)  DEFAULT NULL COMMENT '目标类型：AUTH / USER / ROLE / PERMISSION / SELF',
    `target_id`    varchar(64)  DEFAULT NULL COMMENT '目标ID（用户ID、角色ID；登录类动作为账号）',
    `target_name`  varchar(128) DEFAULT NULL COMMENT '目标名称（账号/角色名/权限名）',
    `http_method`  varchar(8)   DEFAULT NULL COMMENT 'HTTP 方法',
    `request_uri`  varchar(255) DEFAULT NULL COMMENT '请求路径（不含查询串）',
    `client_ip`    varchar(64)  DEFAULT NULL COMMENT '客户端IP（优先 X-Forwarded-For 首跳）',
    `result`       tinyint      NOT NULL DEFAULT 0 COMMENT '结果：0 成功 / 1 失败',
    `error_code`   int          DEFAULT NULL COMMENT '失败时的业务码或 HTTP 状态码',
    `error_msg`    varchar(255) DEFAULT NULL COMMENT '失败原因（已截断，不含敏感数据）',
    `cost_ms`      int          DEFAULT NULL COMMENT '接口耗时（毫秒）',
    `created_time` datetime     NOT NULL COMMENT '发生时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_audit_operator_time` (`operator`, `created_time`),
    KEY `idx_audit_action_time` (`action`, `created_time`),
    KEY `idx_audit_created_time` (`created_time`),
    KEY `idx_audit_trace` (`trace_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '操作审计日志（只追加；登录/登出与关键管理动作）' ROW_FORMAT = DYNAMIC;

-- ============================================================================
-- 二、基础数据种子（与线上基线一致）
-- ============================================================================

-- 1) 菜单/按钮权限：菜单 1..5 + 19；按钮 6..18 + 20（permission_key 与后端注解/前端 v-perm 一致）
--    status：0 启用 / 1 禁用(null 按启用)   show：0 展示 / 1 隐藏
--    菜单统一 status=0(启用)、show=0(展示)；按钮的 show 无意义（无界面元素）故为 NULL
INSERT INTO `sys_auth_permission`
(`id`,`name`,`parent_id`,`order_num`,`type`,`menu_url`,`component`,`backend_url`,`status`,`show`,`icon`,`permission_key`,`created_by`,`created_time`,`is_deleted`)
VALUES
(1,'工作台',0,1,0,'/HomeView','HomeView',NULL,0,0,'el-icon-s-home','home:homeview','admin',NOW(),0),
(2,'系统管理',0,2,0,'/systemManagement','',NULL,0,0,'el-icon-s-operation','system:management','admin',NOW(),0),
(3,'用户管理',2,1,0,'/userInfo','userInfo/UserInfo',NULL,0,0,'el-icon-user','userInfo:userInfo','admin',NOW(),0),
(4,'角色管理',2,2,0,'/roleInfo','userInfo/RoleInfo',NULL,0,0,'el-icon-s-check','userInfo:roleInfo','admin',NOW(),0),
(5,'权限管理',2,3,0,'/permissionInfo','userInfo/PermissionInfo',NULL,0,0,'el-icon-menu','userInfo:permissionInfo','admin',NOW(),0),
(6,'用户查询',3,4,1,'','','/sysAuthUser/paginQuery',0,NULL,'','sysAuthUser:paginQuery','admin',NOW(),0),
(7,'新增用户',3,10,1,'','','/sysAuthUser/addSysAuthUser',0,NULL,'','sysAuthUser:addSysAuthUser','admin',NOW(),0),
(8,'编辑用户',3,11,1,'','','/sysAuthUser/editSysAuthUser',0,NULL,'','sysAuthUser:editSysAuthUser','admin',NOW(),0),
(9,'删除用户',3,12,1,'','','/sysAuthUser/deleteById',0,NULL,'','sysAuthUser:deleteById','admin',NOW(),0),
(10,'重置密码',3,13,1,'','','/sysAuthUser/resetPassword',0,NULL,'','sysAuthUser:resetPassword','admin',NOW(),0),
(11,'分配角色',3,14,1,'','','/sysAuthUser/assignRole',0,NULL,'','sysAuthUser:assignRole','admin',NOW(),0),
(12,'新增角色',4,10,1,'','','/sysAuthRole/addAuthRole',0,NULL,'','sysAuthRole:addAuthRole','admin',NOW(),0),
(13,'编辑角色',4,11,1,'','','/sysAuthRole/editAuthRole',0,NULL,'','sysAuthRole:editAuthRole','admin',NOW(),0),
(14,'删除角色',4,12,1,'','','/sysAuthRole/deleteById',0,NULL,'','sysAuthRole:deleteById','admin',NOW(),0),
(15,'分配权限',4,13,1,'','','/sysAuthRole/assignPermission',0,NULL,'','sysAuthRole:assignPermission','admin',NOW(),0),
(16,'新增权限',5,10,1,'','','/sysAuthPermission/addAuthPermission',0,NULL,'','sysAuthPermission:addAuthPermission','admin',NOW(),0),
(17,'编辑权限',5,11,1,'','','/sysAuthPermission/editAuthPermission',0,NULL,'','sysAuthPermission:editAuthPermission','admin',NOW(),0),
(18,'删除权限',5,12,1,'','','/sysAuthPermission/deleteById',0,NULL,'','sysAuthPermission:deleteById','admin',NOW(),0),
-- V1.0.7 新增：操作审计菜单 + 查询按钮（后端只读接口 /sysAuthAuditLog/paginQuery）
(19,'操作审计',2,4,0,'/auditLog','auditLog/index',NULL,0,0,'el-icon-document','auditLog:auditLog','admin',NOW(),0),
(20,'审计查询',19,1,1,'','','/sysAuthAuditLog/paginQuery',0,NULL,'','sysAuthAuditLog:paginQuery','admin',NOW(),0),
(21,'在线会话',2,5,0,'/onlineSession','onlineSession/index',NULL,0,0,'el-icon-monitor','onlineSession:onlineSession','admin',NOW(),0),
(22,'强制下线',21,1,1,'','','/sysAuthSession/kick',0,NULL,'','sysAuthSession:kick','admin',NOW(),0),
(23,'部门管理',2,6,0,'/dept','dept/index',NULL,0,0,'el-icon-office-building','dept:dept','admin',NOW(),0),
(24,'新增部门',23,1,1,'','','/sysAuthDept/addDept',0,NULL,'','sysAuthDept:addDept','admin',NOW(),0),
(25,'编辑部门',23,2,1,'','','/sysAuthDept/editDept',0,NULL,'','sysAuthDept:editDept','admin',NOW(),0),
(26,'删除部门',23,3,1,'','','/sysAuthDept/deleteById',0,NULL,'','sysAuthDept:deleteById','admin',NOW(),0);

-- 2) 角色：管理员(admin，超级角色) / 测试角色1(cehsi1) / 普通用户(common，注册默认)
INSERT INTO `sys_auth_role` (`id`,`role_name`,`role_key`,`created_by`,`created_time`,`is_deleted`) VALUES
(1,'管理员','admin','admin',NOW(),0),
(2,'测试角色1','cehsi1','admin',NOW(),0),
(3,'普通用户','common','admin',NOW(),0);

-- 3) 角色-权限授权
--    管理员：全部 1..20（虽为超级角色自动放行，仍建关联便于授权页回显）
INSERT INTO `sys_auth_role_permission` (`role_id`,`permission_id`,`created_by`,`created_time`,`is_deleted`) VALUES
(1,1,'admin',NOW(),0),(1,2,'admin',NOW(),0),(1,3,'admin',NOW(),0),(1,4,'admin',NOW(),0),(1,5,'admin',NOW(),0),
(1,6,'admin',NOW(),0),(1,7,'admin',NOW(),0),(1,8,'admin',NOW(),0),(1,9,'admin',NOW(),0),(1,10,'admin',NOW(),0),
(1,11,'admin',NOW(),0),(1,12,'admin',NOW(),0),(1,13,'admin',NOW(),0),(1,14,'admin',NOW(),0),(1,15,'admin',NOW(),0),
(1,16,'admin',NOW(),0),(1,17,'admin',NOW(),0),(1,18,'admin',NOW(),0),
(1,19,'admin',NOW(),0),(1,20,'admin',NOW(),0),
    (1,21,'admin',NOW(),0),(1,22,'admin',NOW(),0),
    (1,23,'admin',NOW(),0),(1,24,'admin',NOW(),0),(1,25,'admin',NOW(),0),(1,26,'admin',NOW(),0),
--    测试角色1：工作台 + 系统管理 + 用户管理/角色管理菜单 + 用户查询按钮（演示按钮级权限）
(2,1,'admin',NOW(),0),(2,2,'admin',NOW(),0),(2,3,'admin',NOW(),0),(2,4,'admin',NOW(),0),(2,6,'admin',NOW(),0),
--    普通用户：仅工作台
(3,1,'admin',NOW(),0);

-- 4) 演示账号（密码均为 12345678；散列取自当前线上库保持一致）
INSERT INTO `sys_auth_user`
(`id`,`user_name`,`nick_name`,`email`,`phone`,`password`,`sex`,`status`,`created_by`,`created_time`,`is_deleted`)
VALUES
(1,'admin','admin','123456@qq.com','12134567890','$2a$10$/VhWe0aRisfDuKKBvxQeruMZ..1TqQZU4wKdlHNqoJjnz58NGCtLm',1,0,'admin',NOW(),0),
(3,'test','test','test@qq.com','17263526352','$2a$10$/VhWe0aRisfDuKKBvxQeruMZ..1TqQZU4wKdlHNqoJjnz58NGCtLm',1,0,'admin',NOW(),0);

-- 5.1) 部门种子（V1.0.9）：总公司 / 技术部 / 产品部，并把演示用户挂上去
INSERT INTO `sys_dept` (`id`,`parent_id`,`dept_name`,`order_num`,`leader`,`phone`,`status`,`created_by`,`created_time`,`is_deleted`) VALUES
(1,0,'总公司',1,'总经理','010-00000001',0,'admin',NOW(),0),
(2,1,'技术部',1,'技术负责人','010-00000002',0,'admin',NOW(),0),
(3,1,'产品部',2,'产品负责人','010-00000003',0,'admin',NOW(),0);

UPDATE `sys_auth_user` SET `dept_id` = 1 WHERE `user_name` = 'admin';
UPDATE `sys_auth_user` SET `dept_id` = 2 WHERE `user_name` = 'test';

-- 6) 用户-角色：admin -> 管理员+测试角色1；test -> 测试角色1
INSERT INTO `sys_auth_user_role` (`user_id`,`role_id`,`created_by`,`created_time`,`is_deleted`) VALUES
(1,1,'admin',NOW(),0),
(1,2,'admin',NOW(),0),
(3,2,'admin',NOW(),0);

-- 自增起点：避免与新注册/新数据冲突
ALTER TABLE `sys_auth_permission` AUTO_INCREMENT = 100;
ALTER TABLE `sys_auth_role`        AUTO_INCREMENT = 100;
ALTER TABLE `sys_auth_user`        AUTO_INCREMENT = 100;
ALTER TABLE `sys_auth_user_role`   AUTO_INCREMENT = 100;
ALTER TABLE `sys_auth_role_permission` AUTO_INCREMENT = 100;

SET FOREIGN_KEY_CHECKS = 1;

-- 核对：
--   SELECT COUNT(*) FROM sys_auth_user;                     -- 期望 2
--   SELECT COUNT(*) FROM sys_auth_role;                     -- 期望 3
--   SELECT COUNT(*) FROM sys_auth_permission;               -- 期望 18
--   SELECT COUNT(*) FROM sys_auth_role_permission;          -- 期望 24(role1=18,role2=5,role3=1)
--   SELECT COUNT(*) FROM sys_auth_user_role WHERE is_deleted=0; -- 期望 3
