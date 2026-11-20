package com.admintem.web.config.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;

/**
 * API 版本化别名过滤器。
 *
 * <p>对 {@code /api/v1/**} 请求剥离版本前缀后再交给 Spring MVC / Security，
 * 因此以下两种地址等价：</p>
 * <pre>
 *   /auth/login
 *   /api/v1/auth/login
 * </pre>
 *
 * <p>过滤器必须早于 Spring Security（{@link Ordered#HIGHEST_PRECEDENCE}），
 * 否则安全规则会看到未剥离的路径，白名单匹配失败。</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiVersionFilter extends OncePerRequestFilter {

    public static final String VERSION_PREFIX = "/api/v1";
    public static final String VERSION_HEADER = "X-API-Version";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String uri = request.getRequestURI();
        String path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
        if (path.equals(VERSION_PREFIX) || path.startsWith(VERSION_PREFIX + "/")) {
            String stripped = path.substring(VERSION_PREFIX.length());
            if (stripped.isEmpty()) {
                stripped = "/";
            }
            String targetUri = contextPath + stripped;
            response.setHeader(VERSION_HEADER, "v1");
            filterChain.doFilter(new VersionedRequest(request, targetUri), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** 只改写路径相关方法，其余请求信息原样透传。 */
    private static final class VersionedRequest extends HttpServletRequestWrapper {

        private final String requestUri;

        private VersionedRequest(HttpServletRequest request, String requestUri) {
            super(request);
            this.requestUri = requestUri;
        }

        @Override
        public String getRequestURI() {
            return requestUri;
        }

        @Override
        public String getServletPath() {
            return requestUri;
        }

        @Override
        public String getPathInfo() {
            return null;
        }

        @Override
        public StringBuffer getRequestURL() {
            try {
                URI original = URI.create(super.getRequestURL().toString());
                String base = original.getScheme() + "://" + original.getRawAuthority();
                return new StringBuffer(base + requestUri);
            } catch (Exception e) {
                return new StringBuffer(requestUri);
            }
        }
    }
}
