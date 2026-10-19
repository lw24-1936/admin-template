package com.admintem.web.security;

import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.service.UserRbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 用户信息服务。
 *
 * <p>在登录认证（DaoAuthenticationProvider 校验密码）时被调用：
 * 按账号查库（自动过滤逻辑删除），账号被禁用时抛出 {@link org.springframework.security.authentication.DisabledException}，
 * 校验通过前即加载好角色与权限，登录成功后直接缓存为会话快照。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    /** 用户 RBAC 加载服务（组合关系，便于复用同一套查库逻辑） */
    private final UserRbacService userRbacService;

    /**
     * 按登录账号加载用户（含角色与权限）。
     *
     * @param username 登录账号
     * @return LoginUser
     * @throws UsernameNotFoundException 用户不存在
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LoginUser loginUser = userRbacService.loadLoginUserSnapshot(null, username);
        if (loginUser == null) {
            log.warn("登录失败：账号 {} 不存在", username);
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        if (!loginUser.isEnabled()) {
            log.warn("登录失败：账号 {} 已被禁用", username);
            throw new org.springframework.security.authentication.DisabledException("账号已被禁用");
        }
        return loginUser;
    }
}
