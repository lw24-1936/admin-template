package com.admintem.web.security;

import com.admintem.web.config.AdmintemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * RBAC 权限判定服务（供 @PreAuthorize 引用，bean 名为 {@code rbac}）。
 *
 * <p>用法示例：{@code @PreAuthorize("@rbac.hasPerm('sysAuthUser:addSysAuthUser')")}</p>
 *
 * <p>判定规则：
 * <ul>
 *   <li>未登录 → 直接拒绝（返回 false 由方法级安全抛出 403）；</li>
 *   <li>拥有超级角色（role_key = admin，可在配置中调整）→ 放行全部；</li>
 *   <li>否则校验当前用户权限快照中是否包含目标 permission_key。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service("rbac")
@RequiredArgsConstructor
public class RbacService {

    private final AdmintemProperties properties;

    /**
     * 判定当前用户是否拥有指定权限。
     *
     * @param permissionKey 权限唯一标识（按钮级）
     * @return true 允许访问
     */
    public boolean hasPerm(String permissionKey) {
        LoginUser current = currentUser();
        if (current == null) {
            return false;
        }
        // 超级角色放行
        if (current.hasRoleKey(properties.getSecurity().getAdminRoleKey())) {
            return true;
        }
        boolean ok = current.hasPermissionKey(permissionKey);
        if (!ok) {
            log.info("权限校验未通过: user={}, missing perm={}", current.getUserName(), permissionKey);
        }
        return ok;
    }

    /**
     * 判定当前用户是否拥有多个权限中的任意一个（用于"多页面共用查询接口"场景）。
     *
     * @param permissionKeys 权限标识数组
     * @return true 表示至少拥有其中一个
     */
    public boolean hasAnyPerm(String... permissionKeys) {
        LoginUser current = currentUser();
        if (current == null) {
            return false;
        }
        if (current.hasRoleKey(properties.getSecurity().getAdminRoleKey())) {
            return true;
        }
        if (permissionKeys == null) {
            return false;
        }
        for (String key : permissionKeys) {
            if (current.hasPermissionKey(key)) {
                return true;
            }
        }
        log.info("权限校验未通过: user={}, missing any of={}", current.getUserName(), String.join(",", permissionKeys));
        return false;
    }

    /**
     * 判定当前用户是否拥有指定角色。
     *
     * @param roleKey 角色唯一标识
     * @return true 表示拥有
     */
    public boolean hasRole(String roleKey) {
        LoginUser current = currentUser();
        return current != null && current.hasRoleKey(roleKey);
    }

    /* ------------------------------ 私有方法 ------------------------------ */

    /** 从安全上下文获取当前登录用户 */
    private LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 未认证或认证对象异常时直接视为未登录
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        // 本工程所有认证主体均为 LoginUser（由 JWT 过滤器写入安全上下文）
        if (!(principal instanceof LoginUser)) {
            return null;
        }
        return (LoginUser) principal;
    }
}
