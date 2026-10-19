package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作审计分页查询条件。
 *
 * <p>时间范围用**字符串**而不是 {@code LocalDateTime}，原因有两个：</p>
 * <ol>
 *   <li>前端日期选择器给出的就是 {@code yyyy-MM-dd HH:mm:ss}，用字符串可以原样接收，
 *       不必依赖全局 Jackson 对 JavaTime 的格式配置（项目里没有额外配置，默认是 ISO）；</li>
 *   <li>解析失败时可以给出明确的中文提示（"时间格式应为 ……"），而不是一个 400 的反序列化异常。</li>
 * </ol>
 *
 * <p>过滤器与索引的对应关系（见 V1.0.5 建表脚本）：
 * {@code operator} → {@code idx_audit_operator_time}、
 * {@code action} → {@code idx_audit_action_time}、
 * 仅时间范围 → {@code idx_audit_created_time}。
 * 因此"按操作人 + 时间倒序"这类常用查询不会退化成全表扫。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作审计分页查询条件")
public class AuditLogPageQuery extends PageQuery {

    /** 操作人账号（模糊匹配） */
    @Schema(description = "操作人账号（模糊）", example = "admin")
    private String operator;

    /** 动作码（精确匹配，如 USER_ADD） */
    @Schema(description = "动作码（精确）", example = "USER_ADD")
    private String action;

    /** 目标名称（模糊匹配，如用户账号、角色名、权限名） */
    @Schema(description = "目标名称（模糊）", example = "test")
    private String targetName;

    /** 结果：0 成功 / 1 失败；null 表示全部 */
    @Schema(description = "结果：0 成功 / 1 失败；不传为全部", example = "0")
    private Integer result;

    /** 起始时间（含），格式 yyyy-MM-dd HH:mm:ss */
    @Schema(description = "起始时间（含），yyyy-MM-dd HH:mm:ss", example = "2026-10-01 00:00:00")
    private String beginTime;

    /** 结束时间（含），格式 yyyy-MM-dd HH:mm:ss */
    @Schema(description = "结束时间（含），yyyy-MM-dd HH:mm:ss", example = "2026-10-31 23:59:59")
    private String endTime;
}
