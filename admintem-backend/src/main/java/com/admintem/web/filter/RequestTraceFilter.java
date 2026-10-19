package com.admintem.web.filter;

import com.admintem.web.config.AdmintemProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求跟踪过滤器（参考 config/log/Filter 的链路跟踪设计）。
 *
 * <p>为每个请求生成唯一 traceId 并放入 SLF4J MDC（日志 pattern 含 {@code %X{traceId}}），
 * 同时写入响应头 {@code X-Trace-Id}；接口层与 SQL 日志共用该 traceId，
 * 一条请求 → 若干 SQL 的执行链路可整体串联，便于排查问题与慢 SQL。</p>
 *
 * <p>说明：实例由 {@code config/log/FilterConfig} 创建并注册，避免重复注册。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RequestTraceFilter extends OncePerRequestFilter {

    private final AdmintemProperties properties;

    /** MDC Key（与 logback-spring.xml pattern 的 %X{traceId} 对应） */
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 生成 traceId（截取 24 位十六进制，长度足够避免碰撞且便于阅读）；
        //    当前不读取上游 traceId，如需网关透传请在此处先读 X-Request-Id 再兜底生成
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(properties.getLog().getTraceHeader(), traceId);

        long start = System.currentTimeMillis();
        try {
            log.info("请求开始: {} {} traceId={}", request.getMethod(), request.getRequestURI(), traceId);
            filterChain.doFilter(request, response);
        } finally {
            log.info("请求完成: {} {} status={} cost={}ms traceId={}",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), System.currentTimeMillis() - start, traceId);
            // 清理 MDC，避免线程池复用串号
            MDC.remove(MDC_TRACE_ID);
        }
    }
}
