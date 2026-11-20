package com.admintem.web.security;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 外部网关 SSO 断言校验（HMAC JWT）。
 *
 * <p>网关使用与后端共享的 secret 签发 JWT，至少包含 {@code sub}（映射本地账号）、
 * {@code jti}（防重放）、{@code iss}、{@code aud}、{@code exp}。本系统不实现 OIDC 授权码流程，
 * 只接受网关已完成认证后的短时断言。</p>
 */
@Component
@RequiredArgsConstructor
public class SsoTokenVerifier {

    private final AdmintemProperties properties;

    public Claims verify(String assertion) {
        AdmintemProperties.Auth.Sso config = properties.getAuth().getSso();
        if (!config.isEnabled()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "SSO 未启用");
        }
        if (!StringUtils.hasText(config.getSecret()) || config.getSecret().length() < 32) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "SSO 密钥未配置或长度不足 32 字节");
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(config.getSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(config.getIssuer())
                    .requireAudience(config.getAudience())
                    .clockSkewSeconds(config.getClockSkewSeconds())
                    .build()
                    .parseSignedClaims(assertion)
                    .getPayload();
            if (!StringUtils.hasText(claims.getSubject()) || !StringUtils.hasText(claims.getId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "SSO 断言缺少 sub 或 jti");
            }
            return claims;
        } catch (BusinessException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "SSO 断言无效或已过期");
        }
    }
}
