package com.admintem.web.aspect;

import com.admintem.web.common.Result;
import com.admintem.web.entity.SysAuthAuditLog;
import com.admintem.web.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审计切面测试：锁定"审计绝不影响业务"这条底线。
 *
 * <p>审计是附加动作，最可怕的失败模式不是"没记上"，而是**记不上导致业务失败**
 * 或**异常被吞掉**。因此这里的断言集中在三处：返回值原样、异常原样上抛、
 * 审计服务抛异常时业务仍正常返回。</p>
 */
class AuditLogAspectTest {

    private AuditLogService auditLogService;
    private AuditLogAspect aspect;
    private AuditLog annotation;

    @BeforeEach
    void setUp() throws Exception {
        auditLogService = mock(AuditLogService.class);
        aspect = new AuditLogAspect(auditLogService);
        annotation = Fixture.class.getDeclaredMethod("addUser")
                .getAnnotation(AuditLog.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sysAuthUser/addSysAuthUser");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("成功：原样返回结果，并写入一条审计")
    void recordsOnSuccess() throws Throwable {
        Result<Long> expected = Result.ok(66L);
        ProceedingJoinPoint joinPoint = mockJoinPoint(expected, null);

        Object actual = aspect.around(joinPoint, annotation);

        assertSame(expected, actual, "切面不得改写返回值");
        ArgumentCaptor<SysAuthAuditLog> captor = ArgumentCaptor.forClass(SysAuthAuditLog.class);
        verify(auditLogService, times(1)).recordAsync(captor.capture());
        SysAuthAuditLog entry = captor.getValue();
        assertEquals("USER_ADD", entry.getAction());
        assertEquals(SysAuthAuditLog.RESULT_SUCCESS, entry.getResult());
        assertEquals("66", entry.getTargetId());
    }

    @Test
    @DisplayName("抛异常：审计仍写入失败记录，且异常原样上抛")
    void recordsAndRethrowsOnFailure() throws Throwable {
        IllegalStateException boom = new IllegalStateException("boom");
        ProceedingJoinPoint joinPoint = mockJoinPoint(null, boom);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> aspect.around(joinPoint, annotation));

        assertSame(boom, thrown, "业务异常必须原样上抛");
        ArgumentCaptor<SysAuthAuditLog> captor = ArgumentCaptor.forClass(SysAuthAuditLog.class);
        verify(auditLogService, times(1)).recordAsync(captor.capture());
        assertEquals(SysAuthAuditLog.RESULT_FAILURE, captor.getValue().getResult());
    }

    @Test
    @DisplayName("审计服务自身抛异常：业务返回值不受影响")
    void auditFailureDoesNotBreakBusiness() throws Throwable {
        Result<Long> expected = Result.ok(7L);
        ProceedingJoinPoint joinPoint = mockJoinPoint(expected, null);
        org.mockito.Mockito.doThrow(new RuntimeException("audit down"))
                .when(auditLogService).recordAsync(org.mockito.ArgumentMatchers.any());

        Object actual = aspect.around(joinPoint, annotation);

        assertSame(expected, actual);
    }

    /** 构造连接点：proceed() 返回给定值或抛出给定异常 */
    private ProceedingJoinPoint mockJoinPoint(Object returnValue, Throwable toThrow) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        if (toThrow != null) {
            when(joinPoint.proceed()).thenThrow(toThrow);
        } else {
            when(joinPoint.proceed()).thenReturn(returnValue);
        }
        return joinPoint;
    }

    /** 注解夹具 */
    private static class Fixture {
        @AuditLog(action = "USER_ADD", name = "新增用户", target = AuditTargetType.USER)
        public void addUser() {
            // 仅用于承载注解
        }
    }
}
