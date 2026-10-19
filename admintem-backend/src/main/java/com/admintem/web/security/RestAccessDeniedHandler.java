package com.admintem.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.admintem.web.common.Result;
import com.admintem.web.common.ResultCode;
import com.admintem.web.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 已登录但无权限访问统一响应（403）。
 *
 * <p>覆盖过滤器链层级的访问拒绝（如路径级规则），
 * 方法级 @PreAuthorize 拒绝则由 {@link com.admintem.web.exception.GlobalExceptionHandler} 统一处理。</p>
 *
 * <p>两处 403 出口都会写一条审计记录（动作码 ACCESS_DENIED）：
 * "权限不足被拒绝"是安全审计中最有价值的信号——它说明有人尝试了授权之外的操作，
 * 而这类请求不会进入业务方法，因此无法由 {@link com.admintem.web.aspect.AuditLogAspect} 覆盖。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    private final AuditLogService auditLogService;

    /**
     * 输出 403 JSON，并记录一次越权访问日志（含账号，便于安全审计）。
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication != null && authentication.getPrincipal() instanceof LoginUser user
                ? user.getUserName() : "anonymous";
        log.warn("越权访问被拒绝: {} {}, user={}", request.getMethod(), request.getRequestURI(), userName);
        // 审计留痕（审计自身失败不会影响响应，见 AuditLogService 约定）
        auditLogService.recordAccessDenied(request, accessDeniedException.getMessage());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), Result.fail(ResultCode.FORBIDDEN));
    }
}
