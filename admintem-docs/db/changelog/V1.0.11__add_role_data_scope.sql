-- ============================================================================
-- V1.0.11 角色数据范围（行级数据权限）
--
-- 值说明：
--   ALL             全部数据（默认，兼容历史行为）
--   DEPT            本部门
--   DEPT_AND_CHILD  本部门及所有子部门
--   SELF            仅本人
--
-- 只新增列，不修改历史数据；已有角色默认 ALL，升级后行为不变。
-- ============================================================================

ALTER TABLE `sys_auth_role`
    ADD COLUMN `data_scope` varchar(20) NOT NULL DEFAULT 'ALL'
        COMMENT '数据范围：ALL/DEPT/DEPT_AND_CHILD/SELF' AFTER `role_key`;

UPDATE `sys_auth_role`
SET `data_scope` = 'ALL'
WHERE `data_scope` IS NULL OR `data_scope` = '';
