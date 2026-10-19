package com.admintem.web.security;

import com.admintem.web.config.AdmintemProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RbacService} 单元测试（鉴权判定核心逻辑）。
 *
 * <p>该类是所有 {@code @PreAuthorize("@rbac.hasPerm('...')")} 的实际判定者，
 * 逻辑虽短但属安全边界：这里锁定"未登录拒绝 / 超级角色放行 / 普通用户按键判定"
 * 三类规则，并覆盖 hasAnyPerm 与 hasRole。</p>
 */
@DisplayName("RBAC 权限判定")
class RbacServiceTest {

    private RbacService rbac;
    private AdmintemProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AdmintemProperties();
        rbac = new RbacService(properties);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 把指定用户写入安全上下文（模拟 JWT 过滤器认证后的状态） */
    private void login(LoginUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private LoginUser userWith(Set<String> roleKeys, Set<String> permissionKeys) {
        LoginUser user = new LoginUser(9L, "t_user", "测试用户", "hash", 0);
        user.setRoleKeys(roleKeys);
        user.setPermissionKeys(permissionKeys);
        return user;
    }

    @Test
    @DisplayName("未登录：一律拒绝")
    void rejectsWhenNotAuthenticated() {
        assertThat(rbac.hasPerm("sysAuthUser:addSysAuthUser")).isFalse();
        assertThat(rbac.hasAnyPerm("a:b", "c:d")).isFalse();
        assertThat(rbac.hasRole("admin")).isFalse();
    }

    @Test
    @DisplayName("认证主体不是 LoginUser（如匿名）时拒绝")
    void rejectsNonLoginUserPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));
        assertThat(rbac.hasPerm("sysAuthUser:addSysAuthUser")).isFalse();
    }

    @Test
    @DisplayName("超级角色（role_key=admin）放行全部权限")
    void adminRoleBypassesPermissionCheck() {
        login(userWith(Set.of("admin"), Set.of()));

        assertThat(rbac.hasPerm("sysAuthUser:addSysAuthUser")).isTrue();
        assertThat(rbac.hasPerm("any:unknownKey")).isTrue();
        assertThat(rbac.hasAnyPerm("a:b", "c:d")).isTrue();
        assertThat(rbac.hasRole("admin")).isTrue();
    }

    @Test
    @DisplayName("超级角色 key 可通过配置调整")
    void adminRoleKeyIsConfigurable() {
        properties.getSecurity().setAdminRoleKey("superAdmin");
        login(userWith(Set.of("superAdmin"), Set.of()));

        assertThat(rbac.hasPerm("sysAuthUser:addSysAuthUser")).isTrue();
        // 旧的 admin 不再具备放行效果
        assertThat(rbac.hasRole("admin")).isFalse();
    }

    @Test
    @DisplayName("普通用户：拥有该权限标识则放行，否则拒绝")
    void normalUserCheckedByPermissionKey() {
        login(userWith(Set.of("cehsi1"), Set.of("userInfo:userInfo", "sysAuthUser:paginQuery")));

        assertThat(rbac.hasPerm("userInfo:userInfo")).isTrue();
        assertThat(rbac.hasPerm("sysAuthUser:paginQuery")).isTrue();
        assertThat(rbac.hasPerm("sysAuthUser:addSysAuthUser")).isFalse();
        assertThat(rbac.hasRole("admin")).isFalse();
    }

    @Test
    @DisplayName("hasAnyPerm：任一命中即放行")
    void hasAnyPermPassesWhenOneMatches() {
        login(userWith(Set.of("cehsi1"), Set.of("userInfo:roleInfo")));

        assertThat(rbac.hasAnyPerm("userInfo:userInfo", "userInfo:roleInfo")).isTrue();
        assertThat(rbac.hasAnyPerm("userInfo:userInfo", "userInfo:permissionInfo")).isFalse();
        assertThat(rbac.hasAnyPerm()).isFalse();
        assertThat(rbac.hasAnyPerm((String[]) null)).isFalse();
    }

    @Test
    @DisplayName("无任何角色/权限的空快照：全部拒绝")
    void rejectsEmptySnapshot() {
        login(userWith(Set.of(), Set.of()));

        assertThat(rbac.hasPerm("userInfo:userInfo")).isFalse();
        assertThat(rbac.hasAnyPerm("userInfo:userInfo")).isFalse();
        assertThat(rbac.hasRole("admin")).isFalse();
    }

    @Test
    @DisplayName("hasRole：按 role_key 精确匹配")
    void hasRoleMatchesExactly() {
        login(userWith(Set.of("cehsi1", "common"), Set.of()));

        assertThat(rbac.hasRole("cehsi1")).isTrue();
        assertThat(rbac.hasRole("common")).isTrue();
        assertThat(rbac.hasRole("admin")).isFalse();
    }
}
