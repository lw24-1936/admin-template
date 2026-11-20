-- ============================================================================
-- V1.0.14 文件中心
--
-- 保存通用附件元数据；实际字节仍由 StorageStrategy 存到 local/MinIO。
-- 安全提示：当前本地实现挂在 /uploads/** 公开目录，只适合公开附件/图片；
-- 合同、证件等敏感文件应改为鉴权下载接口，不能直接放在本目录。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `sys_file` (
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
    `url`           varchar(500) NOT NULL COMMENT '可访问 URL',
    `file_size`     bigint       NOT NULL DEFAULT 0 COMMENT '字节数',
    `content_type`  varchar(128) DEFAULT NULL COMMENT 'MIME 类型',
    `storage_type`  varchar(16)  NOT NULL DEFAULT 'local' COMMENT '存储策略 local/minio',
    `remark`        varchar(255) DEFAULT NULL COMMENT '备注',
    `created_by`    varchar(32)  DEFAULT NULL COMMENT '上传人',
    `created_time`  datetime     DEFAULT NULL COMMENT '上传时间',
    `update_by`     varchar(32)  DEFAULT NULL COMMENT '更新人',
    `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
    `is_deleted`    int          NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_file_created_time` (`created_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '文件中心' ROW_FORMAT = DYNAMIC;

INSERT INTO `sys_auth_permission`
(`id`,`name`,`parent_id`,`order_num`,`type`,`menu_url`,`component`,`backend_url`,`status`,`show`,`icon`,`permission_key`,`created_by`,`created_time`,`is_deleted`)
VALUES
(35,'文件中心',2,9,0,'/sysFile','sysFile/index',NULL,0,0,'el-icon-document','sysFile:sysFile','admin',NOW(),0),
(36,'上传文件',35,1,1,'','','/sysFile/upload',0,NULL,'','sysFile:upload','admin',NOW(),0),
(37,'删除文件',35,2,1,'','','/sysFile/deleteById',0,NULL,'','sysFile:delete','admin',NOW(),0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `update_time` = NOW();

INSERT INTO `sys_auth_role_permission` (`role_id`,`permission_id`,`created_by`,`created_time`,`is_deleted`)
SELECT 1, p.id, 'admin', NOW(), 0
FROM `sys_auth_permission` p
WHERE p.permission_key IN ('sysFile:sysFile','sysFile:upload','sysFile:delete')
  AND p.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM `sys_auth_role_permission` rp
      WHERE rp.role_id = 1 AND rp.permission_id = p.id AND rp.is_deleted = 0
  );
