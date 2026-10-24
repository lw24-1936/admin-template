package com.admintem.web.aspect;

import com.admintem.web.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;

/**
 * 方法级日志切面：controller + service 的公共方法统一打印入参、返回值、异常与耗时。
 *
 * <p>日志格式（traceId 由 logback 的 %X{traceId} 输出）：</p>
 * <pre>
 * 方法调用开始: method=SysAuthUserServiceImpl#page, args=[{...}]
 * 方法调用完成: method=SysAuthUserServiceImpl#page, return={"records":[...]}, cost=18ms
 * 方法调用异常: method=AuthServiceImpl#login, args=[{...}], error=BusinessException: 账号或密码错误, cost=32ms
 * </pre>
 *
 * <p>安全与体积控制：</p>
 * <ul>
 *   <li>密码/令牌等字段由 {@link LogSanitizer} 脱敏，不写明文；</li>
 *   <li>参数与返回值按 {@code admintem.log.aspect.max-length} 截断；</li>
 *   <li>文件只记录名称/大小，不读内容；</li>
 *   <li>超过 {@code slow-ms} 额外打 WARN，方便定位慢方法。</li>
 * </ul>
 *
 * <p>覆盖范围与边界：只拦截 Spring 代理的 public 方法；同类内部 this 调用、
 * private 方法、MyBatis Mapper 不在切面内（Mapper 的 SQL 由 sys-sql 日志单独记录）。
 * 需要完全关闭时设置 {@code admintem.log.aspect.enable=false}。</p>
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "admintem.log.aspect", name = "enable",
        havingValue = "true", matchIfMissing = true)
public class ApiLogAspect {

    private final ObjectMapper objectMapper;

    /** 单段参数/返回值的最大字符数 */
    @Value("${admintem.log.aspect.max-length:1200}")
    private int maxLength;

    /** 慢方法阈值（毫秒），超过则额外 WARN */
    @Value("${admintem.log.aspect.slow-ms:500}")
    private long slowMs;

    /** 切点：controller 与 service 的公共方法 */
    @Pointcut("execution(public * com.admintem.web.controller..*.*(..))"
            + " || execution(public * com.admintem.web.service..*.*(..))")
    public void methodPointcut() {
    }

    @Around("methodPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String method = joinPoint.getTarget().getClass().getSimpleName()
                + "#" + signature.getMethod().getName();
        boolean infoEnabled = log.isInfoEnabled();
        String args = infoEnabled ? describeArgs(joinPoint.getArgs()) : "";

        if (infoEnabled) {
            log.info("方法调用开始: method={}, args={}", method, args);
        }

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;

            if (infoEnabled) {
                log.info("方法调用完成: method={}, return={}, cost={}ms",
                        method, LogSanitizer.describe(result, objectMapper, maxLength), cost);
            }
            if (slowMs > 0 && cost >= slowMs) {
                log.warn("方法调用较慢: method={}, cost={}ms, threshold={}ms", method, cost, slowMs);
            }
            return result;
        } catch (Throwable throwable) {
            long cost = System.currentTimeMillis() - start;
            if (throwable instanceof BusinessException businessException) {
                // 业务异常是预期分支：WARN 记录参数与错误码，堆栈放 DEBUG 避免刷屏
                log.warn("方法调用异常: method={}, args={}, error={}: {}, cost={}ms",
                        method, args, throwable.getClass().getSimpleName(),
                        businessException.getMessage(), cost);
                log.debug("业务异常堆栈: method={}", method, throwable);
            } else {
                // 非预期异常：ERROR + 完整堆栈，便于直接定位
                log.error("方法调用异常: method={}, args={}, cost={}ms", method, args, cost, throwable);
            }
            throw throwable;
        }
    }

    /** 逐个参数脱敏；数组/集合由 Jackson 统一处理 */
    private String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Object arg : args) {
            joiner.add(LogSanitizer.describe(arg, objectMapper, maxLength));
        }
        return LogSanitizer.truncate(joiner.toString(), maxLength);
    }
}
