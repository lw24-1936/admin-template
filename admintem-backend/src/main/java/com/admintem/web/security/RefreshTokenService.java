package com.admintem.web.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 刷新令牌服务（不透明令牌，Redis 存储）。
 *
 * <p>刷新令牌与访问令牌分离：访问令牌短期有效，刷新令牌只用于换取新访问令牌；
 * 每次刷新都会旋转（旧 token 立即失效），降低泄露后的重放窗口。</p>
 *
 * <p>当前仍沿用单账号单访问令牌的白名单模型；刷新令牌独立存储，不改变
 * 现有 JWT 过滤器与在线会话语义，后续可在此基础上扩展 deviceId。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String TOKEN_PREFIX = "admintem:auth:refresh:";
    private static final String USER_SET_PREFIX = TOKEN_PREFIX + "user:";
    private static final long NORMAL_TTL_DAYS = 7;
    private static final long REMEMBER_TTL_DAYS = 30;

    private final RedissonClient redissonClient;

    /**
     * 签发刷新令牌。
     *
     * @param userName 登录账号
     * @param remember 是否记住我
     * @return 随机不透明令牌
     */
    public String issue(String userName, boolean remember) {
        String token = UUID.randomUUID().toString().replace("-", "");
        long ttlSeconds = (remember ? REMEMBER_TTL_DAYS : NORMAL_TTL_DAYS) * 24 * 3600;
        redissonClient.getBucket(TOKEN_PREFIX + token).set(userName, ttlSeconds, TimeUnit.SECONDS);
        RSet<String> userSet = redissonClient.getSet(USER_SET_PREFIX + userName);
        userSet.add(token);
        userSet.expire(ttlSeconds, TimeUnit.SECONDS);
        return token;
    }

    /**
     * 消费（旋转）刷新令牌：成功返回账号，失败返回 null。
     * 消费后旧 token 立即删除，防止重复使用。
     */
    public String consume(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        RBucket<String> bucket = redissonClient.getBucket(TOKEN_PREFIX + token);
        String userName = bucket.get();
        if (!StringUtils.hasText(userName)) {
            return null;
        }
        bucket.delete();
        redissonClient.getSet(USER_SET_PREFIX + userName).remove(token);
        return userName;
    }

    /**
     * 撤销某账号全部刷新令牌（退出、改密、禁用时调用）。
     */
    public void revokeAll(String userName) {
        if (!StringUtils.hasText(userName)) {
            return;
        }
        RSet<String> userSet = redissonClient.getSet(USER_SET_PREFIX + userName);
        for (String token : userSet) {
            redissonClient.getBucket(TOKEN_PREFIX + token).delete();
        }
        userSet.delete();
        log.info("撤销刷新令牌: userName={}", userName);
    }
}
