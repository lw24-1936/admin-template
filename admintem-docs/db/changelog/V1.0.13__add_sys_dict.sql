-- ============================================================================
-- V1.0.13 数据字典
--
-- 单表设计：dict_type + dict_value 唯一，适合模板阶段的下拉/状态字典；
-- 若后续需要"字典类型元数据"再拆 sys_dict_type，不影响现有接口。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `sys_dict` (
    `id`         bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dict_type`  varchar(32)  NOT NULL COMMENT '字典类型',
    `dict_label` varchar(64)  NOT NULL COMMENT '显示文案',
    `dict_value` varchar(64)  NOT NULL COMMENT '字典值',
    `sort_num`   int          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`     tinyint      NOT NULL DEFAULT 0 COMMENT '状态 0启用 1禁用',
    `remark`     varchar(255) DEFAULT NULL COMMENT '备注',
    `created_by` varchar(32)  DEFAULT NULL COMMENT '创建人',
    `created_time` datetime   DEFAULT NULL COMMENT '创建时间',
    `update_by`  varchar(32)  DEFAULT NULL COMMENT '更新人',
    `update_time` datetime    DEFAULT NULL COMMENT '更新时间',
    `is_deleted` int          NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_dict_type` (`dict_type`),
    UNIQUE KEY `uk_dict_type_value_active`
        ((IF(`is_deleted` = 0, CONCAT(`dict_type`, ':', `dict_value`), NULL)))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '数据字典' ROW_FORMAT = DYNAMIC;

INSERT INTO `sys_auth_permission`
(`id`,`name`,`parent_id`,`order_num`,`type`,`menu_url`,`component`,`backend_url`,`status`,`show`,`icon`,`permission_key`,`created_by`,`created_time`,`is_deleted`)
VALUES
(31,'字典管理',2,8,0,'/sysDict','sysDict/index',NULL,0,0,'el-icon-document','sysDict:sysDict','admin',NOW(),0),
(32,'新增字典',31,1,1,'','','/sysDict/add',0,NULL,'','sysDict:add','admin',NOW(),0),
(33,'编辑字典',31,2,1,'','','/sysDict/edit',0,NULL,'','sysDict:edit','admin',NOW(),0),
(34,'删除字典',31,3,1,'','','/sysDict/deleteById',0,NULL,'','sysDict:delete','admin',NOW(),0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `update_time` = NOW();

INSERT INTO `sys_auth_role_permission` (`role_id`,`permission_id`,`created_by`,`created_time`,`is_deleted`)
SELECT 1, p.id, 'admin', NOW(), 0
FROM `sys_auth_permission` p
WHERE p.permission_key IN ('sysDict:sysDict','sysDict:add','sysDict:edit','sysDict:delete')
  AND p.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM `sys_auth_role_permission` rp
      WHERE rp.role_id = 1 AND rp.permission_id = p.id AND rp.is_deleted = 0
  );
