package com.admintem.web.security;

import com.admintem.web.exception.BusinessException;
import com.admintem.web.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具类。
 *
 * <p>供控制器/服务获取当前请求的用户信息（由 JWT 过滤器写入 SecurityContext）。</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户对象。
     *
     * @return LoginUser
     * @throws BusinessException 未登录时抛出 401 业务异常
     */
    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser user)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户ID
     */
    public static Long currentUserId() {
        return currentUser().getUserId();
    }

    /**
     * 获取当前登录用户账号。
     *
     * @return 账号
     */
    public static String currentUserName() {
        return currentUser().getUserName();
    }
}
