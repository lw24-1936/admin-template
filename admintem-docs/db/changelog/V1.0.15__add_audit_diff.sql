-- ============================================================================
-- V1.0.15 审计变更快照（before/after）
--
-- 仅用于"配置/字典等可管理数据"的显式变更快照；敏感参数值需在业务层脱敏后再写入。
-- 审计表仍保持只追加，不新增 update/delete 语句。
-- ============================================================================

ALTER TABLE `sys_auth_audit_log`
    ADD COLUMN `before_data` LONGTEXT NULL COMMENT '变更前快照（JSON，已脱敏）' AFTER `error_msg`,
    ADD COLUMN `after_data`  LONGTEXT NULL COMMENT '变更后快照（JSON，已脱敏）' AFTER `before_data`;
