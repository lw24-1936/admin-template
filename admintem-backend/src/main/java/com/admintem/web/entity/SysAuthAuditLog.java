package com.admintem.web.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作审计日志实体（sys_auth_audit_log）。
 *
 * <p>本表只追加、不修改：没有 is_deleted 字段，Mapper 也只提供 insert。
 * 这样"审计记录本身"不会被业务逻辑改写，可作为独立追溯依据。</p>
 *
 * <p>与其它留痕手段的分工：</p>
 * <ul>
 *   <li>应用日志（logs/admintem-info.log）：排障用，按天滚动、会被清理；</li>
 *   <li>业务表的 created_by/update_by：只能看到"最后一次是谁改的"；</li>
 *   <li>本表：一次操作一行，含操作人、动作、目标、结果、耗时、traceId、IP，
 *       可回答"某条权限是谁在什么时候移除的、成功了还是被拒绝"。</li>
 * </ul>
 */
@Data
@Schema(description = "操作审计日志")
public class SysAuthAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Schema(description = "主键")
    private Long id;

    /** 链路ID（与请求日志、SQL 日志共用同一 traceId） */
    @Schema(description = "链路ID")
    private String traceId;

    /** 操作人账号（登录类动作记录被尝试的账号） */
    @Schema(description = "操作人账号")
    private String operator;

    /** 操作人用户ID（匿名请求为 null） */
    @Schema(description = "操作人用户ID")
    private Long operatorId;

    /** 动作码（如 LOGIN / USER_ADD / ACCESS_DENIED） */
    @Schema(description = "动作码")
    private String action;

    /** 动作中文名 */
    @Schema(description = "动作中文名")
    private String actionName;

    /** 目标类型（AUTH / USER / ROLE / PERMISSION / SELF / ACCESS） */
    @Schema(description = "目标类型")
    private String targetType;

    /** 目标ID（用户ID、角色ID；登录类动作为账号） */
    @Schema(description = "目标ID")
    private String targetId;

    /** 目标名称（账号 / 角色名 / 权限名，便于人读） */
    @Schema(description = "目标名称")
    private String targetName;

    /** HTTP 方法 */
    @Schema(description = "HTTP 方法")
    private String httpMethod;

    /** 请求路径（不含查询串，避免敏感参数入库） */
    @Schema(description = "请求路径")
    private String requestUri;

    /** 客户端IP（优先取 X-Forwarded-For 首跳） */
    @Schema(description = "客户端IP")
    private String clientIp;

    /** 结果：0 成功 / 1 失败 */
    @Schema(description = "结果：0 成功 / 1 失败")
    private Integer result;

    /** 失败时的业务码或 HTTP 状态码 */
    @Schema(description = "失败码")
    private Integer errorCode;

    /** 失败原因（已截断，不记录口令/令牌等敏感数据） */
    @Schema(description = "失败原因")
    private String errorMsg;

    /** 接口耗时（毫秒） */
    @Schema(description = "耗时（毫秒）")
    private Integer costMs;

    /** 发生时间 */
    @Schema(description = "发生时间")
    private LocalDateTime createdTime;

    /* ---------------- 便于阅读的常量（避免各处写魔法值） ---------------- */

    /** 结果：成功 */
    public static final int RESULT_SUCCESS = 0;

    /** 结果：失败 */
    public static final int RESULT_FAILURE = 1;
}
