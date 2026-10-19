package com.admintem.web.aspect;

import com.admintem.web.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 接口调用日志切面。
 *
 * <p>对 controller 包下所有公共方法统一打印调用耗时与结果状态，
 * 便于全链路审计与问题定位。</p>
 *
 * <p>开关：`admintem.log.aspect.enable`（默认 true），沿用原项目
 * `log.aspect.enable` 的设计，可随时关闭以降低日志量。</p>
 *
 * <p>安全说明：不打印请求体与响应体中的敏感数据（密码/令牌等），
 * 仅记录 类名.方法名 + 结果码 + 耗时。</p>
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(prefix = "admintem.log.aspect", name = "enable",
        havingValue = "true", matchIfMissing = true)
public class ApiLogAspect {

    /** 切点：所有 controller 公共方法 */
    @Pointcut("execution(public * com.admintem.web.controller..*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：记录接口调用日志。
     *
     * @param joinPoint 连接点
     * @return 原始返回值（不修改业务结果）
     * @throws Throwable 原样上抛异常，由全局异常处理器兜底
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            log.warn("接口调用异常: method={}, cost={}ms, error={}",
                    method, System.currentTimeMillis() - start, t.getMessage());
            throw t;
        }
        long cost = System.currentTimeMillis() - start;
        // 仅记录结果状态码，避免打印令牌等敏感数据
        if (result instanceof Result<?> r) {
            log.info("接口调用完成: method={}, code={}, cost={}ms", method, r.getCode(), cost);
        } else {
            log.info("接口调用完成: method={}, cost={}ms", method, cost);
        }
        return result;
    }
}
