package com.admintem.web.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 刷新令牌服务（不透明令牌，Redis 存储）。
 *
 * <p>刷新令牌与访问令牌分离：访问令牌短期有效，刷新令牌只用于换取新访问令牌；
 * 每次刷新都会旋转（旧 token 立即失效），降低泄露后的重放窗口。</p>
 *
 * <p>刷新令牌可绑定 deviceId；解绑设备时只撤销该设备的刷新令牌，
 * 账号维度仍可用 revokeAll 一次清空全部设备。</p>
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
        return issue(userName, remember, null);
    }

    /**
     * 签发刷新令牌（可绑定设备）。
     *
     * <p>Redis value 形如 {@code userName|deviceId}；旧数据没有 {@code |} 时按单会话处理。</p>
     */
    public String issue(String userName, boolean remember, String deviceId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        long ttlSeconds = (remember ? REMEMBER_TTL_DAYS : NORMAL_TTL_DAYS) * 24 * 3600;
        String value = userName + "|" + (deviceId == null ? "" : deviceId);
        redissonClient.getBucket(TOKEN_PREFIX + token).set(value, ttlSeconds, TimeUnit.SECONDS);
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
        RefreshSession session = consumeSession(token);
        return session == null ? null : session.userName();
    }

    /**
     * 消费（旋转）刷新令牌并返回绑定的账号/设备。
     */
    public RefreshSession consumeSession(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        RBucket<String> bucket = redissonClient.getBucket(TOKEN_PREFIX + token);
        String value = bucket.get();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        int separator = value.indexOf('|');
        String userName = separator < 0 ? value : value.substring(0, separator);
        String deviceId = separator < 0 || separator == value.length() - 1
                ? null : value.substring(separator + 1);
        bucket.delete();
        redissonClient.getSet(USER_SET_PREFIX + userName).remove(token);
        return new RefreshSession(userName, deviceId);
    }

    /** 刷新令牌绑定的会话。 */
    public record RefreshSession(String userName, String deviceId) {
    }

    /**
     * 撤销某账号全部刷新令牌（退出、改密、禁用时调用）。
     */
    public void revoke(String userName, String deviceId) {
        if (!StringUtils.hasText(userName) || !StringUtils.hasText(deviceId)) {
            revokeAll(userName);
            return;
        }
        RSet<String> userSet = redissonClient.getSet(USER_SET_PREFIX + userName);
        List<String> toRemove = new ArrayList<>();
        for (String token : userSet) {
            RBucket<String> bucket = redissonClient.getBucket(TOKEN_PREFIX + token);
            String value = bucket.get();
            if (value != null && value.endsWith("|" + deviceId)) {
                toRemove.add(token);
            }
        }
        for (String token : toRemove) {
            redissonClient.getBucket(TOKEN_PREFIX + token).delete();
            userSet.remove(token);
        }
        log.info("撤销设备刷新令牌: userName={}, deviceId={}, count={}", userName, deviceId, toRemove.size());
    }

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
