package com.admintem.web.security;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 安全事件指标（供 Prometheus/Alertmanager 使用）。
 *
 * <p>只做计数，不记录账号/令牌/IP 等敏感标签，避免基数爆炸与隐私泄露。
 * 需要按账号排查时使用审计表与 traceId。</p>
 */
@Component
@RequiredArgsConstructor
public class SecurityMetrics {

    private final MeterRegistry meterRegistry;

    /** 未登录/令牌失效导致的 401。 */
    public void unauthorized() {
        meterRegistry.counter("admintem_security_requests_total",
                "result", "unauthorized").increment();
    }

    /**
     * 越权访问导致的 403。
     *
     * @param layer filter（路径级）或 method（@PreAuthorize 方法级）
     */
    public void accessDenied(String layer) {
        meterRegistry.counter("admintem_security_requests_total",
                "result", "forbidden", "layer", layer).increment();
    }

    /**
     * 登录结果计数（不含账号标签）。
     *
     * @param result success / failure
     */
    public void login(String result) {
        meterRegistry.counter("admintem_login_total", "result", result).increment();
    }
}
