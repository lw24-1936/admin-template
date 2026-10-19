package com.admintem.web.aspect;

import com.admintem.web.entity.SysAuthAuditLog;
import com.admintem.web.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 操作审计切面：为标注 {@link AuditLog} 的控制器方法写审计记录。
 *
 * <p>与 {@link ApiLogAspect} 的分工：ApiLogAspect 切的是"所有控制器方法"，
 * 目的是运行期排障（打印耗时与结果码，不落库）；本切面只切被显式注解的方法，
 * 目的是**审计留痕**（写库、可长期查询）。两者切点不同、用途不同，故分开。</p>
 *
 * <p>三条硬约束（审计是"附加动作"，不能影响主流程）：</p>
 * <ol>
 *   <li>无论成功、业务失败还是抛异常，都要记一条：写在 finally 中；</li>
 *   <li>审计自身异常绝不上抛：构造失败只告警，业务返回值/异常原样放行；</li>
 *   <li>不改写返回值、不吞业务异常（异常在 catch 中记录后原样抛出）。</li>
 * </ol>
 *
 * <p>开关：{@code admintem.audit.enable}。开关只作用于**持久化边界**
 * （{@link com.admintem.web.service.impl.AuditLogServiceImpl} 早退），
 * 切面本身保持无条件注册——这样"是否落库"只有一处判断，不会出现
 * "切面关了但 403 出口还在写"这类半开半关的状态。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    /**
     * 环绕通知：执行原方法并记录审计。
     *
     * @param joinPoint 连接点
     * @param auditLog  方法上的审计注解
     * @return 原方法返回值
     * @throws Throwable 原方法异常（原样上抛，由全局异常处理器兜底）
     */
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                SysAuthAuditLog entry = AuditLogFactory.actionRecord(
                        auditLog, joinPoint.getArgs(), result, error, System.currentTimeMillis() - start);
                auditLogService.recordAsync(entry);
            } catch (Exception e) {
                // 构造审计记录本身出错（例如入参反射异常）：只告警，绝不改变业务结果
                log.warn("审计记录构造失败（已忽略）: action={}, err={}", auditLog.action(), e.getMessage());
            }
        }
    }
}
