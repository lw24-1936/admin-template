package com.admintem.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.admintem.web.common.Result;
import com.admintem.web.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未登录/令牌失效统一响应（401）。
 *
 * <p>取代默认的跳转登录页行为，向客户端返回统一 JSON 结构，
 * 前端据此清除本地令牌并跳转登录页。</p>
 *
 * <p>这里是"认证失败"的统一出口，必须留日志（含请求方法与路径），
 * 否则令牌过期/被注销导致的 401 只能从请求行状态码侧推断，无从定位账号与原因。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final SecurityMetrics securityMetrics;

    /**
     * 输出 401 JSON。
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("认证失败，拒绝访问: {} {}, reason={}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());
          securityMetrics.unauthorized();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), Result.fail(ResultCode.UNAUTHORIZED));
    }
}
