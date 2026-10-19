package com.admintem.web.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 操作审计列表项（给前端直接渲染用）。
 *
 * <p>与实体 {@link com.admintem.web.entity.SysAuthAuditLog} 的差异：</p>
 * <ul>
 *   <li>时间在服务端格式化为 {@code yyyy-MM-dd HH:mm:ss}：前端不必再写一遍格式化，
 *       也避免每个页面各自处理 ISO 与本地时区；</li>
 *   <li>补一个 {@code resultText}（成功/失败）：表格直接显示中文，不用在前端写 0/1 映射。</li>
 * </ul>
 */
@Data
@Schema(description = "操作审计列表项")
public class AuditLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "链路ID（可与应用日志、SQL 日志对齐）")
    private String traceId;

    @Schema(description = "操作人账号")
    private String operator;

    @Schema(description = "操作人用户ID（匿名请求为空）")
    private Long operatorId;

    @Schema(description = "动作码")
    private String action;

    @Schema(description = "动作中文名")
    private String actionName;

    @Schema(description = "目标类型")
    private String targetType;

    @Schema(description = "目标ID")
    private String targetId;

    @Schema(description = "目标名称")
    private String targetName;

    @Schema(description = "HTTP 方法")
    private String httpMethod;

    @Schema(description = "请求路径")
    private String requestUri;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "结果：0 成功 / 1 失败")
    private Integer result;

    @Schema(description = "结果中文")
    private String resultText;

    @Schema(description = "失败码")
    private Integer errorCode;

    @Schema(description = "失败原因（已截断）")
    private String errorMsg;

    @Schema(description = "耗时（毫秒）")
    private Integer costMs;

    @Schema(description = "发生时间 yyyy-MM-dd HH:mm:ss")
    private String createdTime;
}
