package com.admintem.web.service;

import com.admintem.web.entity.SysAuthAuditLog;
import com.admintem.web.entity.dto.AuditLogPageQuery;
import com.admintem.web.entity.vo.AuditLogVO;
import com.admintem.web.entity.vo.PageResult;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

/**
 * 操作审计服务。
 *
 * <p>写路径的唯一职责：把审计记录写进库，且**永不因审计失败影响业务**
 * （写入异常只记 WARN 日志）。因此写入方法不声明受检异常、也不返回失败标识。</p>
 *
 * <p>读路径（{@link #pageQuery} / {@link #queryActionDict}）供审计查询页使用：
 * 审计数据是"只追加"的，读取不会修改任何状态。</p>
 */
public interface AuditLogService {

    /**
     * 写入一条审计记录（同步）。
     *
     * <p>保留同步入口是因为：① 单测可直接断言；② 异步路径最终也走这里，
     * 若线程池拒绝（CallerRunsPolicy）同样会回到调用线程执行。</p>
     *
     * @param entry 审计记录（为 null 时忽略）
     */
    void record(SysAuthAuditLog entry);

    /**
     * 异步写入一条审计记录（切面与 403 出口使用此入口）。
     *
     * <p>审计是旁路能力，不应让业务响应等待落库；实现上用独立线程池串行写入，
     * 队列满时由调用线程执行（不丢审计）。异常处理与 {@link #record} 一致。</p>
     *
     * @param entry 审计记录（为 null 时忽略）
     */
    void recordAsync(SysAuthAuditLog entry);

    /**
     * 记录一次"越权被拒绝"（403）。
     *
     * <p>供两处权限拒绝出口调用：过滤器链层级的 {@code AccessDeniedHandler}
     * 与方法级 {@code @PreAuthorize} 的异常处理器。</p>
     *
     * @param request 当前请求（可取路径与 IP；可为 null）
     * @param reason  拒绝原因（权限表达式或框架提示）
     */
    void recordAccessDenied(HttpServletRequest request, String reason);

    /**
     * 分页查询审计记录（供审计查询页）。
     *
     * @param query 过滤条件（操作人/动作/目标/结果/时间范围 + 分页）
     * @return 分页结果（时间已格式化为 yyyy-MM-dd HH:mm:ss）
     */
    PageResult<AuditLogVO> pageQuery(AuditLogPageQuery query);

    /**
     * 动作字典：已出现过的动作码与中文名，供筛选下拉使用。
     *
     * @return 每项含 action / actionName / total
     */
    List<Map<String, Object>> queryActionDict();
}
