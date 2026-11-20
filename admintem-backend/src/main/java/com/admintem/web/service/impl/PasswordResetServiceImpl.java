package com.admintem.web.service.impl;

import com.admintem.web.common.ResultCode;
import com.admintem.web.entity.SysAuthUser;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysAuthUserMapper;
import com.admintem.web.security.JwtTokenService;
import com.admintem.web.security.PasswordPolicy;
import com.admintem.web.security.RbacCacheService;
import com.admintem.web.security.RefreshTokenService;
import com.admintem.web.service.PasswordResetNotifier;
import com.admintem.web.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 密码找回：验证码存 Redis，单次消费，5 次输错失效，60 秒内不可重复请求。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String CODE_KEY_PREFIX = "admintem:auth:pwdreset:code:";
    private static final String REQUEST_KEY_PREFIX = "admintem:auth:pwdreset:req:";
    private static final long CODE_TTL_SECONDS = 600L;
    private static final long RESEND_SECONDS = 60L;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SysAuthUserMapper userMapper;
    private final RedissonClient redissonClient;
    private final PasswordResetNotifier notifier;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final RbacCacheService rbacCacheService;

    @Override
    public void requestCode(String userName) {
        if (!StringUtils.hasText(userName)) {
            return;
        }
        SysAuthUser user = userMapper.selectByUserName(userName.trim());
        // 不存在也返回成功语义；只有真实账号才写 Redis/发码
        if (user == null) {
            log.info("密码找回请求：账号不存在，按统一语义忽略");
            return;
        }
        RBucket<String> requestBucket = redissonClient.getBucket(REQUEST_KEY_PREFIX + user.getUserName());
        if (!requestBucket.setIfAbsent("1", Duration.ofSeconds(RESEND_SECONDS))) {
            log.info("密码找回请求过于频繁，忽略: userName={}", user.getUserName());
            return;
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String value = DigestUtils.md5DigestAsHex(code.getBytes()) + "|0";
        redissonClient.getBucket(CODE_KEY_PREFIX + user.getUserName())
                .set(value, CODE_TTL_SECONDS, TimeUnit.SECONDS);
        notifier.send(user, code);
        log.info("密码找回验证码已生成: userName={}", user.getUserName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reset(String userName, String code, String newPassword) {
        if (!StringUtils.hasText(userName) || !StringUtils.hasText(code)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码无效或已过期");
        }
        SysAuthUser user = userMapper.selectByUserName(userName.trim());
        RBucket<String> bucket = redissonClient.getBucket(CODE_KEY_PREFIX + userName.trim());
        String stored = bucket.get();
        if (user == null || stored == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码无效或已过期");
        }
        String[] parts = stored.split("\\|", -1);
        int attempts = parts.length > 1 ? parseInt(parts[1]) : 0;
        if (attempts >= MAX_ATTEMPTS) {
            bucket.delete();
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码尝试次数过多，请重新获取");
        }
        String expected = DigestUtils.md5DigestAsHex(code.getBytes());
        if (parts.length < 1 || !expected.equals(parts[0])) {
            long ttl = bucket.remainTimeToLive();
            if (ttl > 0) {
                bucket.set(expected + "|" + (attempts + 1), ttl, TimeUnit.MILLISECONDS);
            }
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误或已过期");
        }
        passwordPolicy.validateComplexity(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateBy("password-reset");
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateSelective(user);
        bucket.delete();
        redissonClient.getBucket(REQUEST_KEY_PREFIX + user.getUserName()).delete();
        jwtTokenService.revokeToken(user.getUserName());
        refreshTokenService.revokeAll(user.getUserName());
        rbacCacheService.evictUser(user.getId());
        log.info("密码找回成功，已吊销全部会话: userId={}", user.getId());
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
