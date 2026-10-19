package com.admintem.web.config.security;

import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.security.JwtAuthenticationFilter;
import com.admintem.web.security.RestAccessDeniedHandler;
import com.admintem.web.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security 6 配置（无状态 JWT + RBAC）。
 *
 * <p>要点：
 * <ul>
 *   <li>关闭 CSRF/Session/表单登录/HTTP Basic（纯 API 服务）；</li>
 *   <li>白名单（登录、注册、Swagger 等）permitAll，其余请求必须已认证；</li>
 *   <li>按钮级权限通过 {@code @PreAuthorize("@rbac.hasPerm('xxx')")} 方法级控制
 *       （{@code @EnableMethodSecurity} 开启）；</li>
 *   <li>JWT 过滤器置于用户名密码过滤器之前；401/403 统一输出 JSON。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final AdmintemProperties properties;

    /**
     * 密码编码器：BCrypt（与旧项目一致，兼容既有密码散列）。
     *
     * <p>代价因子取配置 {@code admintem.auth.password.bcrypt-strength}（默认 10，生产可提到 12）。
     * 提高强度只影响新密码；老散列由 {@link com.admintem.web.security.PasswordPolicy#needsRehash}
     * 在登录成功时静默升级。</p>
     *
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        int strength = properties.getAuth().getPassword().getBcryptStrength();
        // 越界值收敛到合法范围，避免"配置写错导致启动即崩"
        int safe = Math.min(31, Math.max(4, strength));
        if (safe != strength) {
            log.warn("admintem.auth.password.bcrypt-strength={} 越界，已收敛为 {}", strength, safe);
        }
        return new BCryptPasswordEncoder(safe);
    }

    /**
     * 显式声明 DaoAuthenticationProvider：使用自定义 UserDetailsService 与密码编码器。
     * 该 Bean 会被 {@link AuthenticationConfiguration} 自动聚合进全局 AuthenticationManager。
     */
    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                         PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // 不隐藏"用户不存在"，便于返回精确提示（账号/密码错误统一在 AuthService 转换）
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    /**
     * 认证管理器（登录接口通过它执行账号密码认证）。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * 安全过滤链。
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF（无 Cookie 会话，纯 Token 认证）
                .csrf(AbstractHttpConfigurer::disable)
                // 关闭表单登录与 HTTP Basic
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 启用自定义 CORS（见 CorsConfig）
                .cors(cors -> {
                })
                // 无状态：不创建、不使用 Session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 授权规则
                .authorizeHttpRequests(auth -> {
                    // 1. 白名单（登录、注册、接口文档等）
                    for (String pattern : whitelist()) {
                        auth.requestMatchers(pattern).permitAll();
                    }
                    // 2. 其余接口一律需要登录
                    auth.anyRequest().authenticated();
                })
                // 401 / 403 统一 JSON 响应
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // JWT 认证过滤器放在用户名密码过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 汇总免登录白名单：配置项 + 框架自带文档路径。
     *
     * @return 白名单 Ant 路径集合
     */
    private List<String> whitelist() {
        List<String> urls = new ArrayList<>(properties.getSecurity().getPermitUrls());
        // Swagger / OpenAPI 文档
        urls.add("/v3/api-docs/**");
        urls.add("/swagger-ui/**");
        urls.add("/swagger-ui.html");
        urls.add("/swagger-resources/**");
        urls.add("/webjars/**");
        urls.add("/favicon.ico");
        // 错误页
        urls.add("/error");
        // 头像等上传文件的静态访问：只读图片，公开可访问（不要在此目录放敏感文件）
        urls.add(properties.getUpload().getUrlPrefix() + "/**");
        // 探活：只放行 health（含 liveness/readiness），其余 actuator 端点仍需登录，
        // 且 application.yml 里已限制只暴露 health/info
        urls.add("/actuator/health");
        urls.add("/actuator/health/**");
        return urls;
    }
}
