package com.admintem.web.security;

import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.constant.SecurityConstants;
import com.admintem.web.service.UserRbacService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器。
 *
 * <p>每个请求执行一次：
 * <ol>
 *   <li>从请求头中取出 '&lt;前缀&gt;&lt;jwt&gt;' 并去掉前缀（请求头名与前缀取配置
 *       {@code admintem.jwt.header} / {@code admintem.jwt.bearer-prefix}，
 *       未配置时回落到 {@link SecurityConstants} 常量，默认 {@code admintem_token} + {@code "LAUNCH "}）；</li>
 *   <li>校验签名与有效期（{@link JwtTokenService#parseToken}）；</li>
 *   <li>校验 Redis 令牌白名单一致（服务端可强制下线）；</li>
 *   <li>从缓存/数据库恢复当前登录用户快照，写入 Spring Security 上下文。</li>
 * </ol>
 * 任一环节失败都直接放行（不设置认证信息），由 Security 的
 * {@code AuthenticationEntryPoint} 对受保护接口统一返回 401 JSON。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AdmintemProperties properties;
    private final JwtTokenService jwtTokenService;
    private final RbacCacheService rbacCacheService;
    private final UserRbacService userRbacService;

    /**
     * 过滤主逻辑。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 取令牌（请求头名/前缀以配置为准，配置缺失时回落 SecurityConstants 常量）
        String headerName = StringUtils.hasText(properties.getJwt().getHeader())
                ? properties.getJwt().getHeader() : SecurityConstants.TOKEN_HEADER;
        String tokenPrefix = properties.getJwt().getBearerPrefix() != null
                ? properties.getJwt().getBearerPrefix() : SecurityConstants.TOKEN_PREFIX;
        String header = request.getHeader(headerName);
        if (!StringUtils.hasText(header) || !header.startsWith(tokenPrefix)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(tokenPrefix.length());

        try {
            // 2. 解析并校验签名/有效期
            Claims claims = jwtTokenService.parseToken(token);
            String userName = claims.getSubject();
            Number userIdNum = claims.get(SecurityConstants.CLAIM_USER_ID, Number.class);
            Long userId = userIdNum == null ? null : userIdNum.longValue();
            if (!StringUtils.hasText(userName) || userId == null) {
                log.debug("JWT 载荷缺少关键信息，跳过认证");
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 一次 Redis 往返取回"白名单令牌 + 用户快照"（MGET，避免每请求 2 个 RTT）
            RbacCacheService.SessionSnapshot session = rbacCacheService.readSession(userName, userId);

            // 3.1 白名单校验（与 Redis 中保存的令牌一致）：不一致说明已注销或异地登录
            if (session.token() == null || !session.token().equals(token)) {
                log.debug("JWT 白名单校验失败（已注销或异地登录），userName={}", userName);
                filterChain.doFilter(request, response);
                return;
            }

            // 4. 使用快照（缓存未命中则回源数据库并回填）
            LoginUser loginUser = session.user();
            if (loginUser == null) {
                loginUser = userRbacService.loadLoginUserSnapshot(userId, userName);
                if (loginUser == null || !loginUser.isEnabled()) {
                    log.debug("登录用户不存在或已禁用，跳过认证: userId={}", userId);
                    filterChain.doFilter(request, response);
                    return;
                }
                rbacCacheService.save(loginUser);
            } else if (!loginUser.isEnabled()) {
                filterChain.doFilter(request, response);
                return;
            }

            // 5. 写入安全上下文
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            // 令牌异常（过期/非法等）不做拦截，交由 AuthenticationEntryPoint 统一返回 401
            log.debug("JWT 认证失败: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
