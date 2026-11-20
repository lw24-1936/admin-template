-- ============================================================================
-- V1.0.16 用户 MFA（TOTP）
--
-- 只存 Base32 secret 与启用标记；secret 不返回给前端（setup 时除外），
-- 关闭/重置时通过 updateMfa 显式置空。
-- ============================================================================

ALTER TABLE `sys_auth_user`
    ADD COLUMN `mfa_secret`  varchar(64) NULL COMMENT 'TOTP Base32 secret' AFTER `ext_json`,
    ADD COLUMN `mfa_enabled` tinyint NOT NULL DEFAULT 0 COMMENT 'MFA 0关闭 1开启' AFTER `mfa_secret`;
