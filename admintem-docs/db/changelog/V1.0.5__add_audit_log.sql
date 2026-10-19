-- ============================================================================
-- V1.0.5__add_audit_log.sql
--
-- 目的：新增操作审计日志表 sys_auth_audit_log，把"谁在什么时候对什么做了什么、
--       结果如何"落到数据库，作为权限系统之外的独立追溯依据。
--
-- 背景（为什么需要它）：
--   1. 目前"操作留痕"分散在三处且都不适合作为审计依据：
--      - 应用日志（logs/admintem-info.log）：按天滚动、会被清理，且非结构化；
--      - 慢 SQL 日志：只关心性能；
--      - 业务表里的 created_by/update_by：只能看到"最后一次是谁改的"，
--        无法回答"这条权限是谁在什么时候移除的、当时失败还是成功"。
--   2. 权限系统本身需要"改权限的行为可追溯"——例如某角色被移除某菜单后，
--      需要能查到是哪次操作、由谁执行、是否成功（含被拒绝的尝试）。
--   3. 因此审计表设计为**只追加**：不做逻辑删除、不提供业务更新入口，
--      避免"审计记录本身被改写"。
--
-- 说明：
--   - 表结构无 is_deleted 字段：审计记录只追加，不删除（如需归档请用 04 手册的清理 SQL）；
--   - result：0 成功 / 1 失败；失败时记录 error_code 与 error_msg（已截断且不含口令等敏感数据）；
--   - trace_id 与请求日志/SQL 日志共用同一 traceId，可把"一次请求的接口 + SQL + 审计"串起来；
--   - 索引按审计最常见的三种查法建立：按操作人+时间、按动作+时间、按时间倒序；
--   - 脚本幂等：CREATE TABLE IF NOT EXISTS，可重复执行。
--
-- 执行方式（推荐 source，避免通过管道传中文注释时出现解析异常）：
--   mysql --default-character-set=utf8mb4 -h<host> -P3306 -u<user> -p admintem_new ^
--         -e "source db/changelog/V1.0.5__add_audit_log.sql"
--
-- 对应文档：03-数据库设计与变更说明（变更清单 V1.0.5）、02-系统设计文档（第 13 节 操作审计）
-- ============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_auth_audit_log` (
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `trace_id`     varchar(64)  DEFAULT NULL COMMENT '链路ID（与请求日志/SQL日志共用，便于串联一次请求）',
    `operator`     varchar(32)  DEFAULT NULL COMMENT '操作人账号（登录类动作记录被尝试的账号）',
    `operator_id`  bigint       DEFAULT NULL COMMENT '操作人用户ID（匿名请求为 NULL）',
    `action`       varchar(64)  NOT NULL COMMENT '动作码，如 LOGIN / LOGOUT / USER_ADD / ROLE_ASSIGN_PERM',
    `action_name`  varchar(64)  DEFAULT NULL COMMENT '动作中文名，如「新增用户」',
    `target_type`  varchar(32)  DEFAULT NULL COMMENT '目标类型：AUTH / USER / ROLE / PERMISSION / SELF',
    `target_id`    varchar(64)  DEFAULT NULL COMMENT '目标ID（用户ID、角色ID；登录类动作为账号）',
    `target_name`  varchar(128) DEFAULT NULL COMMENT '目标名称（账号/角色名/权限名，便于人读）',
    `http_method`  varchar(8)   DEFAULT NULL COMMENT 'HTTP 方法',
    `request_uri`  varchar(255) DEFAULT NULL COMMENT '请求路径（不含查询串，避免敏感参数入库）',
    `client_ip`    varchar(64)  DEFAULT NULL COMMENT '客户端IP（优先取 X-Forwarded-For 首跳）',
    `result`       tinyint      NOT NULL DEFAULT 0 COMMENT '结果：0 成功 / 1 失败',
    `error_code`   int          DEFAULT NULL COMMENT '失败时的业务码或 HTTP 状态码',
    `error_msg`    varchar(255) DEFAULT NULL COMMENT '失败原因（已截断，不记录口令/令牌）',
    `cost_ms`      int          DEFAULT NULL COMMENT '接口耗时（毫秒）',
    `created_time` datetime     NOT NULL COMMENT '发生时间',
    PRIMARY KEY (`id`) USING BTREE,
    -- 审计查法一：某人在某段时间做了什么
    KEY `idx_audit_operator_time` (`operator`, `created_time`),
    -- 审计查法二：某类动作在某段时间的分布（例如"删除权限"是否频繁）
    KEY `idx_audit_action_time` (`action`, `created_time`),
    -- 审计查法三：最近发生了什么（时间倒序翻页）
    KEY `idx_audit_created_time` (`created_time`),
    -- 排障查法：由 traceId 反查该请求对应的审计记录
    KEY `idx_audit_trace` (`trace_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '操作审计日志（只追加；登录/登出与关键管理动作）' ROW_FORMAT = DYNAMIC;
