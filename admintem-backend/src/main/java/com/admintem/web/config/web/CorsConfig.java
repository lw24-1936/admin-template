package com.admintem.web.config.web;

import com.admintem.web.config.AdmintemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域配置。
 *
 * <p>开发阶段前端（桌面端 5173 / 移动端 5174）与后端（8090）分端口部署，需要放开跨域；
 * 生产环境通常由 Nginx 同源反代，应通过配置收紧为实际域名或不放开任何跨域来源。</p>
 *
 * <p>允许来源由 {@code admintem.security.cors-allowed-origins} 控制（逗号分隔的 Ant 通配），
 * 留空表示不放开跨域；不再硬编码在代码里，避免生产沿用开发期的 {@code localhost:*} 放行。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final AdmintemProperties properties;

    /**
     * 构造 CORS 规则源。
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的前端来源：来自配置（开发默认 localhost 通配，生产留空即不放开跨域）
        List<String> origins = parseOrigins(properties.getSecurity().getCorsAllowedOrigins());
        if (origins.isEmpty()) {
            log.info("跨域放行列表为空：仅接受同源请求（生产推荐由 Nginx 同源反代）");
        } else {
            log.info("跨域放行来源: {}", origins);
        }
        config.setAllowedOriginPatterns(origins);
        // 允许携带认证信息（token 走自定义请求头，此处统一放开）
        config.setAllowCredentials(true);
        // 允许的请求方法
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许的请求头（含自定义令牌头 admintem_token）
        config.setAllowedHeaders(List.of("*"));
        // 暴露给前端的响应头：admintem_token 为兼容保留；traceId 响应头（默认 X-Trace-Id，
        // 由 RequestTraceFilter 写入）必须暴露，否则跨域下浏览器读不到，前端无法凭 traceId 反馈问题
        String traceHeader = StringUtils.hasText(properties.getLog().getTraceHeader())
                ? properties.getLog().getTraceHeader() : "X-Trace-Id";
        config.setExposedHeaders(List.of("admintem_token", traceHeader));
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 解析逗号分隔的来源列表（去空白、去空项）。
     *
     * @param raw 配置原文，可为空
     * @return 来源列表；无有效项时返回空列表（表示不放开跨域）
     */
    private List<String> parseOrigins(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        List<String> origins = new ArrayList<>();
        for (String item : raw.split(",")) {
            String origin = item.trim();
            if (!origin.isEmpty()) {
                origins.add(origin);
            }
        }
        return origins;
    }
}
