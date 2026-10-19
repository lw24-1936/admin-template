package com.admintem.web.config.log;

import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.filter.RequestTraceFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 请求级过滤器注册（参考 config/log/FilterConfig）。
 *
 * <p>当前注册：{@link RequestTraceFilter}（traceId 链路），开关为
 * {@code admintem.log.trace-enable}（默认 true，见 AdmintemProperties.Log）。</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "admintem.log", name = "trace-enable",
        havingValue = "true", matchIfMissing = true)
public class FilterConfig {

    /**
     * 注册 traceId 过滤器（拦截所有请求）。
     *
     * @param properties 业务配置（读取 traceId 响应头名称等）
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<RequestTraceFilter> traceFilter(AdmintemProperties properties) {
        FilterRegistrationBean<RequestTraceFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestTraceFilter(properties));
        registration.addUrlPatterns("/*");
        // 置顶执行，保证 traceId 对后续安全链/日志可见
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }
}
