package com.admintem.web.aspect;

import com.admintem.web.common.Result;
import com.admintem.web.common.ResultCode;
import com.admintem.web.entity.SysAuthAuditLog;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.filter.RequestTraceFilter;
import com.admintem.web.security.LoginUser;
import com.admintem.web.util.web.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计记录构造工厂：把"一次接口调用"或"一次越权拒绝"转成审计实体。
 *
 * <p>为什么单独抽出来：审计的关键在于**字段口径统一**。若分散在切面与两处 403 出口里
 * 各写一遍，很容易出现"有的记录带 IP、有的不带"这类漂移，也让单元测试变难。
 * 这里集中构造，切面与安全处理器只负责调用。</p>
 *
 * <p>字段口径：</p>
 * <ul>
 *   <li>operator：已登录取当前登录账号；匿名（登录/注册）取被尝试的账号；都没有则 anonymous；</li>
 *   <li>targetId / targetName：由 {@link AuditTargetResolver} 从入参解析；</li>
 *   <li>requestUri：只记路径不记查询串，避免把令牌/密码之类参数写进库；</li>
 *   <li>errorMsg：异常或失败提示原文，由服务层按配置截断；</li>
 *   <li>traceId：取自 MDC，与请求日志、SQL 日志同一值，可整链路串联。</li>
 * </ul>
 */
public final class AuditLogFactory {

    /** 未登录且无目标账号时的占位操作人 */
    private static final String ANONYMOUS = "anonymous";

    private AuditLogFactory() {
    }

    /**
     * 构造"接口动作"审计记录（成功 / 业务失败 / 抛异常三种情况共用）。
     *
     * @param auditLog 注解声明（动作码、名称、目标类型）
     * @param args     方法入参
     * @param result   方法返回值（异常时为 null）
     * @param error    抛出的异常（成功时为 null）
     * @param costMs   耗时（毫秒）
     * @return 审计实体
     */
    public static SysAuthAuditLog actionRecord(AuditLog auditLog, Object[] args,
                                               Object result, Throwable error, long costMs) {
        AuditTarget target = AuditTargetResolver.resolve(args);
        AuditOperator operator = resolveOperator(target);
        AuditOutcome outcome = resolveOutcome(result, error);
        HttpServletRequest request = currentRequest();

        SysAuthAuditLog entry = baseRecord(request, outcome, costMs);
        entry.setAction(auditLog.action());
        entry.setActionName(auditLog.name());
        entry.setTargetType(auditLog.target().name());
        entry.setTargetId(resolveTargetId(target, result));
        entry.setTargetName(target.name());
        entry.setOperator(operator.name());
        entry.setOperatorId(operator.id());
        return entry;
    }

    /**
     * 构造"越权被拒绝（403）"审计记录。
     *
     * <p>为什么单独记：动作注解切面只覆盖"方法真的被调用了"的场景，
     * 而权限不足时方法根本不会执行（@PreAuthorize 在调用前就拒绝了），
     * 这类"被拒绝的尝试"恰恰是审计里最有价值的信号，因此在两处 403 出口单独记录。</p>
     *
     * @param request 当前请求
     * @param reason  拒绝原因（权限表达式或框架提示）
     * @return 审计实体
     */
    public static SysAuthAuditLog accessDeniedRecord(HttpServletRequest request, String reason) {
        SysAuthAuditLog entry = baseRecord(request, new AuditOutcome(false, ResultCode.FORBIDDEN.getCode(), reason), 0L);
        AuditOperator operator = resolveOperator(AuditTarget.EMPTY);
        entry.setAction("ACCESS_DENIED");
        entry.setActionName("越权访问被拒绝");
        entry.setTargetType(AuditTargetType.ACCESS.name());
        entry.setTargetId(null);
        entry.setTargetName(request == null ? null : request.getRequestURI());
        entry.setOperator(operator.name());
        entry.setOperatorId(operator.id());
        return entry;
    }

    /* ------------------------------ 内部实现 ------------------------------ */

    /** 构造公共字段（时间、traceId、请求信息、结果、耗时） */
    private static SysAuthAuditLog baseRecord(HttpServletRequest request, AuditOutcome outcome, long costMs) {
        SysAuthAuditLog entry = new SysAuthAuditLog();
        entry.setCreatedTime(LocalDateTime.now());
        entry.setTraceId(MDC.get(RequestTraceFilter.MDC_TRACE_ID));
        entry.setResult(outcome.success() ? SysAuthAuditLog.RESULT_SUCCESS : SysAuthAuditLog.RESULT_FAILURE);
        entry.setErrorCode(outcome.success() ? null : outcome.code());
        entry.setErrorMsg(outcome.success() ? null : outcome.message());
        entry.setCostMs((int) Math.min(Math.max(costMs, 0L), Integer.MAX_VALUE));
        if (request != null) {
            entry.setHttpMethod(request.getMethod());
            entry.setRequestUri(request.getRequestURI());
            entry.setClientIp(clientIp(request));
        }
        return entry;
    }

    /**
     * 解析目标ID：优先入参里的 id；入参没有时用返回值兜底。
     *
     * <p>为什么需要兜底：新增类接口的目标（新用户/新角色）在调用前并不存在，
     * 其 ID 只在返回值里（{@code Result<Long>}）。若不看返回值，
     * "谁新增了哪个用户"这条审计就只剩账号名、缺了主键，事后关联会变困难。</p>
     */
    static String resolveTargetId(AuditTarget target, Object result) {
        if (target.id() != null) {
            return target.id();
        }
        if (result instanceof Result<?> body && body.getData() instanceof Number id) {
            return id.toString();
        }
        return null;
    }

    /**
     * 解析当前操作人。
     *
     * <p>直接读 SecurityContextHolder 而不调用 {@code SecurityUtils.currentUserName()}：
     * 后者在匿名请求（登录/注册）下会抛未登录异常，而这里需要的是"能取到就取、取不到就退化"。</p>
     */
    static AuditOperator resolveOperator(AuditTarget target) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginUser user) {
                return new AuditOperator(user.getUserName(), user.getUserId());
            }
        } catch (RuntimeException ignored) {
            // 安全上下文不可用时按匿名处理，审计不应因此中断
        }
        String fallback = target != null && target.name() != null ? target.name() : ANONYMOUS;
        return new AuditOperator(fallback, null);
    }

    /**
     * 解析执行结果：优先看异常，其次看统一响应体 code。
     */
    static AuditOutcome resolveOutcome(Object result, Throwable error) {
        if (error != null) {
            int code = error instanceof BusinessException be ? be.getCode() : ResultCode.INTERNAL_ERROR.getCode();
            return new AuditOutcome(false, code, error.getMessage());
        }
        if (result instanceof Result<?> body) {
            return new AuditOutcome(body.isSuccess(), body.getCode(), body.isSuccess() ? null : body.getMessage());
        }
        // 非统一响应体（当前无此类注解方法）：按成功记录，不臆测失败
        return new AuditOutcome(true, ResultCode.SUCCESS.getCode(), null);
    }

    /**
     * 取客户端IP：统一走 {@link ClientIpUtils}（与登录风控同一口径）。
     *
     * <p>历史上这里曾自己实现一遍解析，一旦与风控的取值口径不一致，
     * 就会出现"审计记录的 IP 与风控统计的 IP 对不上"，排查时非常被动。</p>
     */
    static String clientIp(HttpServletRequest request) {
        return ClientIpUtils.getClientIp(request);
    }

    /** 当前请求（非 Web 线程下为 null，例如定时任务直接调用控制器方法） */
    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 操作人（账号 + 用户ID）。
     *
     * @param name 账号
     * @param id   用户ID（匿名时为 null）
     */
    record AuditOperator(String name, Long id) {
    }

    /**
     * 执行结果。
     *
     * @param success 是否成功
     * @param code    业务码/HTTP 状态码
     * @param message 失败提示
     */
    record AuditOutcome(boolean success, Integer code, String message) {
    }
}
