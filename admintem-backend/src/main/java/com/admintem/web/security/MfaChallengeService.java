package com.admintem.web.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MFA 登录挑战（密码通过后的短时票据）。
 *
 * <p>挑战不是 JWT，只存在于 Redis，5 分钟有效，绑定 deviceId，单次消费；
 * 校验失败累加 attempts，超过 5 次即失效，避免无限撞码。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaChallengeService {

    private static final String KEY_PREFIX = "admintem:auth:mfa:";
    private static final long TTL_SECONDS = 300L;
    private static final int MAX_ATTEMPTS = 5;

    private final RedissonClient redissonClient;

    public String create(String userName, String deviceId, boolean remember) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String value = userName + "|" + (deviceId == null ? "" : deviceId) + "|" + (remember ? "1" : "0") + "|0";
        redissonClient.getBucket(KEY_PREFIX + token).set(value, TTL_SECONDS, TimeUnit.SECONDS);
        return token;
    }

    public Challenge read(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        RBucket<String> bucket = redissonClient.getBucket(KEY_PREFIX + token);
        String value = bucket.get();
        if (value == null) {
            return null;
        }
        String[] parts = value.split("\\|", -1);
        if (parts.length < 4) {
            bucket.delete();
            return null;
        }
        return new Challenge(parts[0], parts[1].isEmpty() ? null : parts[1],
                "1".equals(parts[2]), parseInt(parts[3]));
    }

    /** 失败次数 +1，保留剩余 TTL；达到上限直接删除。 */
    public void recordFailure(String token) {
        RBucket<String> bucket = redissonClient.getBucket(KEY_PREFIX + token);
        Challenge challenge = read(token);
        if (challenge == null) {
            return;
        }
        int attempts = challenge.attempts() + 1;
        if (attempts >= MAX_ATTEMPTS) {
            bucket.delete();
            log.warn("MFA 挑战失败次数过多，已失效: userName={}", challenge.userName());
            return;
        }
        long ttlMs = bucket.remainTimeToLive();
        String value = challenge.userName() + "|" + (challenge.deviceId() == null ? "" : challenge.deviceId())
                + "|" + (challenge.remember() ? "1" : "0") + "|" + attempts;
        if (ttlMs > 0) {
            bucket.set(value, ttlMs, TimeUnit.MILLISECONDS);
        }
    }

    public void delete(String token) {
        redissonClient.getBucket(KEY_PREFIX + token).delete();
    }

    public int maxAttempts() {
        return MAX_ATTEMPTS;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 挑战绑定的登录上下文。 */
    public record Challenge(String userName, String deviceId, boolean remember, int attempts) {
    }
}
