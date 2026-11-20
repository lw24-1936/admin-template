package com.admintem.web.service.impl;

import com.admintem.web.common.ResultCode;
import com.admintem.web.entity.SysAuthUser;
import com.admintem.web.entity.dto.SsoLoginRequest;
import com.admintem.web.entity.vo.LoginVO;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysAuthUserMapper;
import com.admintem.web.security.JwtTokenService;
import com.admintem.web.security.LoginUser;
import com.admintem.web.security.RbacCacheService;
import com.admintem.web.security.RefreshTokenService;
import com.admintem.web.security.SsoTokenVerifier;
import com.admintem.web.service.SsoService;
import com.admintem.web.service.AuthService;
import com.admintem.web.service.UserRbacService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;

/** SSO 断言登录实现：校验签名/iss/aud/exp，Redis jti 防重放，然后复用本地 RBAC 快照。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoServiceImpl implements SsoService {

    private static final String REPLAY_PREFIX = "admintem:auth:sso:jti:";

    private final SsoTokenVerifier verifier;
    private final SysAuthUserMapper userMapper;
    private final UserRbacService userRbacService;
    private final AuthService authService;
    private final RbacCacheService rbacCacheService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(SsoLoginRequest request) {
        Claims claims = verifier.verify(request.getAssertion());
        String userName = claims.getSubject();
        String jti = claims.getId();
        Date expiration = claims.getExpiration();
        long ttlSeconds = expiration == null ? 300L
                : Math.max(1L, (expiration.getTime() - System.currentTimeMillis()) / 1000L + 60L);
        RBucket<String> replay = redissonClient.getBucket(REPLAY_PREFIX + jti);
        if (!replay.setIfAbsent(userName, Duration.ofSeconds(ttlSeconds))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "SSO 断言已使用");
        }
        SysAuthUser user = userMapper.selectByUserName(userName);
        if (user == null || !Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "SSO 账号不存在或已禁用");
        }
        LoginUser loginUser = userRbacService.loadLoginUserSnapshot(user.getId(), user.getUserName());
        if (loginUser == null || !loginUser.isEnabled()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        rbacCacheService.save(loginUser);
        boolean remember = Boolean.TRUE.equals(request.getRememberMe());
        String token = jwtTokenService.createToken(user.getId(), user.getUserName(), remember, request.getDeviceId());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setRefreshToken(refreshTokenService.issue(user.getUserName(), remember, request.getDeviceId()));
        vo.setUser(authService.buildSession(user.getId(), loginUser));
        log.info("SSO 登录成功: userName={}, jti={}", user.getUserName(), jti);
        return vo;
    }
}
