package com.admintem.web.aspect;

import com.admintem.web.common.Result;
import com.admintem.web.common.ResultCode;
import com.admintem.web.entity.SysAuthAuditLog;
import com.admintem.web.entity.dto.UserSaveRequest;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.filter.RequestTraceFilter;
import com.admintem.web.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 审计记录构造测试（不起 Spring 上下文、不连库）。
 *
 * <p>覆盖三类容易写错的地方：</p>
 * <ol>
 *   <li>结果判定：成功 / 业务失败（Result.code != 200）/ 抛异常（含 403、500）；</li>
 *   <li>操作人：已登录取登录账号；匿名（登录接口）取被尝试的账号；</li>
 *   <li>请求信息：IP 取 X-Forwarded-For 首跳、URI 不含查询串、traceId 取自 MDC。</li>
 * </ol>
 */
class AuditLogFactoryTest {

    /** 注解实例（通过反射从下面这个被注解的方法上取，顺带验证 RUNTIME 保留） */
    private static final AuditLog ANNOTATION = readAnnotation();

    private static AuditLog readAnnotation() {
        try {
            Method method = Fixture.class.getDeclaredMethod("addUser", UserSaveRequest.class);
            AuditLog annotation = method.getAnnotation(AuditLog.class);
            assertNotNull(annotation, "测试夹具方法必须带 @AuditLog 注解");
            return annotation;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("测试夹具方法缺失", e);
        }
    }

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sysAuthUser/addSysAuthUser");
        request.setRemoteAddr("172.18.0.9");
        request.setQueryString("?token=should-not-be-recorded");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put(RequestTraceFilter.MDC_TRACE_ID, "trace-abc");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        MDC.remove(RequestTraceFilter.MDC_TRACE_ID);
    }

    @Test
    @DisplayName("成功：result=0、无失败码，字段口径齐全")
    void successRecord() {
        loginAs(1L, "admin");
        UserSaveRequest request = new UserSaveRequest();
        request.setUserName("t_new");

        SysAuthAuditLog entry = AuditLogFactory.actionRecord(
                ANNOTATION, new Object[]{request}, Result.ok(88L), null, 12L);

        assertEquals("USER_ADD", entry.getAction());
        assertEquals("新增用户", entry.getActionName());
        assertEquals(AuditTargetType.USER.name(), entry.getTargetType());
        assertEquals("admin", entry.getOperator());
        assertEquals(1L, entry.getOperatorId());
        assertEquals("t_new", entry.getTargetName());
        // 新增类动作的目标ID只存在于返回值中
        assertEquals("88", entry.getTargetId());
        assertEquals(SysAuthAuditLog.RESULT_SUCCESS, entry.getResult());
        assertNull(entry.getErrorCode());
        assertNull(entry.getErrorMsg());
        assertEquals(12, entry.getCostMs());
        assertEquals("POST", entry.getHttpMethod());
        assertEquals("/sysAuthUser/addSysAuthUser", entry.getRequestUri());
        assertEquals("172.18.0.9", entry.getClientIp());
        assertEquals("trace-abc", entry.getTraceId());
        assertNotNull(entry.getCreatedTime());
    }

    @Test
    @DisplayName("业务失败：记录业务码与提示（HTTP 仍是 200）")
    void businessFailureRecord() {
        loginAs(1L, "admin");
        UserSaveRequest request = new UserSaveRequest();
        request.setUserName("admin");

        SysAuthAuditLog entry = AuditLogFactory.actionRecord(
                ANNOTATION, new Object[]{request}, Result.fail(ResultCode.USERNAME_EXISTS, "账号已存在"), null, 5L);

        assertEquals(SysAuthAuditLog.RESULT_FAILURE, entry.getResult());
        assertEquals(ResultCode.USERNAME_EXISTS.getCode(), entry.getErrorCode());
        assertEquals("账号已存在", entry.getErrorMsg());
    }

    @Test
    @DisplayName("抛业务异常：记录异常携带的业务码")
    void businessExceptionRecord() {
        loginAs(1L, "admin");

        SysAuthAuditLog entry = AuditLogFactory.actionRecord(
                ANNOTATION, new Object[0], null, new BusinessException(ResultCode.FORBIDDEN), 3L);

        assertEquals(SysAuthAuditLog.RESULT_FAILURE, entry.getResult());
        assertEquals(ResultCode.FORBIDDEN.getCode(), entry.getErrorCode());
        assertEquals("admin", entry.getOperator());
    }

    @Test
    @DisplayName("抛未知异常：按 500 记录，不透出堆栈")
    void unknownExceptionRecord() {
        SysAuthAuditLog entry = AuditLogFactory.actionRecord(
                ANNOTATION, new Object[0], null, new IllegalStateException("boom"), 1L);

        assertEquals(ResultCode.INTERNAL_ERROR.getCode(), entry.getErrorCode());
        assertEquals("boom", entry.getErrorMsg());
    }

    @Test
    @DisplayName("匿名登录：操作人退化为被尝试的账号（而不是 anonymous）")
    void anonymousLoginOperator() {
        // 登录接口执行时还没有认证信息
        SecurityContextHolder.clearContext();
        com.admintem.web.entity.dto.LoginRequest login = new com.admintem.web.entity.dto.LoginRequest();
        login.setUsername("test");

        SysAuthAuditLog entry = AuditLogFactory.actionRecord(
                ANNOTATION, new Object[]{login}, Result.ok(), null, 20L);

        assertEquals("test", entry.getOperator());
        assertNull(entry.getOperatorId());
        assertEquals("test", entry.getTargetName());
    }

    @Test
    @DisplayName("IP 取 X-Forwarded-For 首跳（反代场景）")
    void forwardedIpWins() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        SysAuthAuditLog entry = AuditLogFactory.actionRecord(ANNOTATION, new Object[0], Result.ok(), null, 1L);

        assertEquals("203.0.113.7", entry.getClientIp());
    }

    @Test
    @DisplayName("越权记录：动作 ACCESS_DENIED、结果失败、目标为请求路径")
    void accessDeniedRecord() {
        loginAs(3L, "test");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sysAuthUser/addSysAuthUser");

        SysAuthAuditLog entry = AuditLogFactory.accessDeniedRecord(request, "拒绝访问");

        assertEquals("ACCESS_DENIED", entry.getAction());
        assertEquals("越权访问被拒绝", entry.getActionName());
        assertEquals(AuditTargetType.ACCESS.name(), entry.getTargetType());
        assertEquals(SysAuthAuditLog.RESULT_FAILURE, entry.getResult());
        assertEquals(ResultCode.FORBIDDEN.getCode(), entry.getErrorCode());
        assertEquals("test", entry.getOperator());
        assertEquals(3L, entry.getOperatorId());
        assertEquals("/sysAuthUser/addSysAuthUser", entry.getTargetName());
        assertEquals("POST", entry.getHttpMethod());
    }

    @Test
    @DisplayName("非 Web 线程（无请求上下文）：不抛异常，请求字段为空")
    void withoutRequestContext() {
        RequestContextHolder.resetRequestAttributes();

        SysAuthAuditLog entry = AuditLogFactory.actionRecord(ANNOTATION, new Object[0], Result.ok(), null, 1L);

        assertNull(entry.getHttpMethod());
        assertNull(entry.getRequestUri());
        assertNull(entry.getClientIp());
        assertNotNull(entry.getCreatedTime());
    }

    /** 写入安全上下文（模拟已登录） */
    private void loginAs(Long userId, String userName) {
        LoginUser loginUser = new LoginUser(userId, userName, userName, null, 0);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    /** 注解夹具：只为在测试里拿到一个真实的 @AuditLog 实例 */
    private static class Fixture {
        @AuditLog(action = "USER_ADD", name = "新增用户", target = AuditTargetType.USER)
        public void addUser(UserSaveRequest request) {
            // 仅用于承载注解
        }
    }
}
