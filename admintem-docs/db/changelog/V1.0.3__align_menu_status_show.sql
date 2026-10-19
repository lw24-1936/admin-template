-- ============================================================================
-- V1.0.3__align_menu_status_show.sql
-- 目的：让 sys_auth_permission 的 `status` / `show` 两个字段与列注释的语义一致，
--       并配合后端过滤逻辑真正生效。
--
-- 背景（本次代码全检发现）：
--   1. 列注释写明 `status`：0 启用 / 1 禁用(null 按启用)；`show`：0 展示 / 1 隐藏；
--      前端「权限管理」界面也为菜单提供了「展示状态（展示/隐藏）」单选与「状态（启用/禁用）」单选。
--   2. 但基线数据里 5 个菜单的 `show` 全部为 1（按注释即"隐藏"），3~5 号菜单的 `status` 为 NULL，
--      而代码此前未按这两个字段过滤，于是"隐藏/禁用"设置形同不存在，数据与注释互相矛盾。
--   3. 处置：数据归一到文档语义（0/0），同时后端按 status 过滤禁用的菜单、
--      前端按 show 决定是否在导航中显示（隐藏菜单仍可直接通过 URL 访问，符合"隐藏菜单"的常规语义）。
--
-- 影响：本脚本只把 5 个基线菜单的 status/show 归一到 0（启用、展示），
--       与修正前的实际界面表现完全一致，不改变任何现有可见行为。
-- 回滚：UPDATE sys_auth_permission SET show=1, status=NULL WHERE type=0 AND id BETWEEN 3 AND 5;
--       UPDATE sys_auth_permission SET show=1 WHERE type=0 AND id IN (1,2);
-- 执行：mysql -h<host> -P3306 -u<user> -p admintem_new < V1.0.3__align_menu_status_show.sql
-- ============================================================================

UPDATE `sys_auth_permission`
SET `status`    = 0,
    `show`      = 0,
    `update_by` = 'changelog',
    `update_time` = NOW()
WHERE `type` = 0
  AND `is_deleted` = 0
  AND (`status` IS NULL OR `show` IS NULL OR `show` <> 0);

-- 核对：5 个菜单应全部为 status=0、show=0
-- SELECT id, name, `status`, `show`, permission_key
--   FROM sys_auth_permission WHERE type = 0 AND is_deleted = 0 ORDER BY id;
