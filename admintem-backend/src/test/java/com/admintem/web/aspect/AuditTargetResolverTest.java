package com.admintem.web.aspect;

import com.admintem.web.entity.dto.AssignRoleRequest;
import com.admintem.web.entity.dto.IdRequest;
import com.admintem.web.entity.dto.LoginRequest;
import com.admintem.web.entity.dto.PermissionSaveRequest;
import com.admintem.web.entity.dto.UserSaveRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审计目标解析器测试。
 *
 * <p>这组测试锁定的是"审计记录里有没有操作对象"这件事：解析失败不会报错、
 * 只会让审计缺字段，属于典型的静默失效，因此必须用测试把各 DTO 的取值口径钉住。</p>
 */
class AuditTargetResolverTest {

    @Test
    @DisplayName("用户保存请求：取 id 与 userName")
    void resolveUserSaveRequest() {
        UserSaveRequest request = new UserSaveRequest();
        request.setId(9L);
        request.setUserName("t_user");

        AuditTarget target = AuditTargetResolver.resolve(new Object[]{request});

        assertEquals("9", target.id());
        assertEquals("t_user", target.name());
    }

    @Test
    @DisplayName("ID 请求：只取 id，名称为空")
    void resolveIdRequest() {
        IdRequest request = new IdRequest();
        request.setId(12L);

        AuditTarget target = AuditTargetResolver.resolve(new Object[]{request});

        assertEquals("12", target.id());
        assertNull(target.name());
    }

    @Test
    @DisplayName("登录请求：字段是 username（小写 n），必须能被识别")
    void resolveLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");

        AuditTarget target = AuditTargetResolver.resolve(new Object[]{request});

        assertNull(target.id());
        assertEquals("admin", target.name());
    }

    @Test
    @DisplayName("分配角色请求：userId 也能作为目标ID")
    void resolveAssignRoleRequest() {
        AssignRoleRequest request = new AssignRoleRequest();
        request.setUserId(5L);
        request.setRoleIds(List.of(1L, 2L));

        AuditTarget target = AuditTargetResolver.resolve(new Object[]{request});

        assertEquals("5", target.id());
        assertNull(target.name());
    }

    @Test
    @DisplayName("权限保存请求：名称优先取中文名（便于人读）")
    void resolvePermissionSaveRequest() {
        PermissionSaveRequest request = new PermissionSaveRequest();
        request.setName("新增用户");
        request.setPermissionKey("sysAuthUser:addSysAuthUser");

        AuditTarget target = AuditTargetResolver.resolve(new Object[]{request});

        assertNull(target.id());
        assertEquals("新增用户", target.name());
    }

    @Test
    @DisplayName("跳过框架参数：Servlet 请求不参与目标解析")
    void skipInfrastructureArgs() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        UserSaveRequest request = new UserSaveRequest();
        request.setUserName("t_next");

        AuditTarget target = AuditTargetResolver.resolve(new Object[]{servletRequest, request});

        assertEquals("t_next", target.name());
    }

    @Test
    @DisplayName("无参/空参/无匹配属性：返回空目标而不抛异常")
    void resolveEmptyCases() {
        assertTrue(AuditTargetResolver.resolve(null).isEmpty());
        assertTrue(AuditTargetResolver.resolve(new Object[0]).isEmpty());
        assertTrue(AuditTargetResolver.resolve(new Object[]{null, "plain-string"}).isEmpty());
    }
}
