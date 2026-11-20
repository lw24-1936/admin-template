-- ============================================================================
-- V1.0.12 系统参数配置
--
-- 目标：给模板使用者一个可直接复用的参数中心（开关、分页大小、上传限制等），
-- 避免每个项目重复造 sys_config 表与 CRUD。
--
-- 权限：菜单 sysConfig:sysConfig + 增/改/删三个按钮，自动授予 admin 角色。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `sys_config` (
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`   varchar(64)  NOT NULL COMMENT '参数键（唯一）',
    `config_value` varchar(2000) DEFAULT NULL COMMENT '参数值',
    `config_name`  varchar(64)  NOT NULL COMMENT '参数名称',
    `config_group` varchar(32)  DEFAULT 'default' COMMENT '分组',
    `remark`       varchar(255) DEFAULT NULL COMMENT '备注',
    `created_by`   varchar(32)  DEFAULT NULL COMMENT '创建人',
    `created_time` datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`    varchar(32)  DEFAULT NULL COMMENT '更新人',
    `update_time`  datetime     DEFAULT NULL COMMENT '更新时间',
    `is_deleted`   int          NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_config_key` (`config_key`),
    UNIQUE KEY `uk_config_key_active` ((IF(`is_deleted` = 0, `config_key`, NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '系统参数配置' ROW_FORMAT = DYNAMIC;

INSERT INTO `sys_auth_permission`
(`id`,`name`,`parent_id`,`order_num`,`type`,`menu_url`,`component`,`backend_url`,`status`,`show`,`icon`,`permission_key`,`created_by`,`created_time`,`is_deleted`)
VALUES
(27,'系统配置',2,7,0,'/sysConfig','sysConfig/index',NULL,0,0,'el-icon-setting','sysConfig:sysConfig','admin',NOW(),0),
(28,'新增参数',27,1,1,'','','/sysConfig/add',0,NULL,'','sysConfig:add','admin',NOW(),0),
(29,'编辑参数',27,2,1,'','','/sysConfig/edit',0,NULL,'','sysConfig:edit','admin',NOW(),0),
(30,'删除参数',27,3,1,'','','/sysConfig/deleteById',0,NULL,'','sysConfig:delete','admin',NOW(),0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `update_time` = NOW();

INSERT INTO `sys_auth_role_permission` (`role_id`,`permission_id`,`created_by`,`created_time`,`is_deleted`)
SELECT 1, p.id, 'admin', NOW(), 0
FROM `sys_auth_permission` p
WHERE p.permission_key IN ('sysConfig:sysConfig','sysConfig:add','sysConfig:edit','sysConfig:delete')
  AND p.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM `sys_auth_role_permission` rp
      WHERE rp.role_id = 1 AND rp.permission_id = p.id AND rp.is_deleted = 0
  );
