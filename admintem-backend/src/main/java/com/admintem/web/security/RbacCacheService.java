package com.admintem.web.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.admintem.web.config.AdmintemProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 登录用户会话快照缓存（Redis，客户端为 Redisson）。
 *
 * <p>每个登录用户在 Redis 中保存一份 JSON 快照（不含密码），
 * 包含角色与权限标识集合，用于：
 * <ul>
 *   <li>JWT 过滤器恢复当前登录用户（免每次请求查库）；</li>
 *   <li>{@link RbacService} 做按钮级权限判定。</li>
 * </ul>
 * 权限/角色/用户变更时调用 {@link #evictUser(Long)} 等清理接口强制回源数据库。
 * </p>
 */
@Slf4j
@Component
public class RbacCacheService {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final AdmintemProperties properties;

    /**
     * 构造器注入。
     */
    public RbacCacheService(RedissonClient redissonClient,
                            ObjectMapper objectMapper,
                            AdmintemProperties properties) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 写入用户快照。
     *
     * @param loginUser 登录用户（密码字段已被 @JsonIgnore 忽略，不落 Redis）
     */
    public void save(LoginUser loginUser) {
        try {
            String json = objectMapper.writeValueAsString(loginUser);
            RBucket<String> bucket = redissonClient.getBucket(cacheKey(loginUser.getUserId()));
            bucket.set(json, properties.getCache().getUserPermExpireMinutes(), TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("用户快照序列化失败: userId={}", loginUser.getUserId(), e);
        }
    }

    /**
     * 读取用户快照；不存在或反序列化失败返回 null。
     *
     * @param userId 用户ID
     * @return LoginUser 或 null
     */
    public LoginUser get(Long userId) {
        RBucket<String> bucket = redissonClient.getBucket(cacheKey(userId));
        String json = bucket.get();
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LoginUser.class);
        } catch (JsonProcessingException e) {
            // 带上异常对象保留堆栈：字段不兼容与脏数据都会走到这里，只有一行提示无法区分
            log.warn("用户快照反序列化失败，将回源数据库: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 一次 Redis 往返读出"令牌白名单 + 用户快照"。
     *
     * <p><b>为什么合并：</b>认证过滤器原先分两次调用——先校验白名单（1 次 GET），
     * 再取用户快照（1 次 GET），等于每个已登录请求都要 2 个 RTT。这里用
     * {@code RBuckets.get(keys...)}（底层 MGET）一次取回两个 key，RTT 减半；
     * 顺带保证两次读取看到的是同一时刻的值。</p>
     *
     * <p>任一 key 不存在时对应字段为 null，由调用方决定如何处理
     * （令牌缺失=未登录；快照缺失=回源数据库并回填）。</p>
     *
     * @param userName 登录账号（令牌白名单按账号存）
     * @param userId   用户ID（快照按用户ID存）
     * @return 读取结果，永不为 null
     */
    public SessionSnapshot readSession(String userName, Long userId) {
        String tokenKey = properties.getJwt().getRedisKeyPrefix() + userName;
        String permKey = cacheKey(userId);
        Map<String, String> values = redissonClient.getBuckets().get(tokenKey, permKey);
        String token = values == null ? null : values.get(tokenKey);
        String json = values == null ? null : values.get(permKey);
        LoginUser user = null;
        if (json != null) {
            try {
                user = objectMapper.readValue(json, LoginUser.class);
            } catch (JsonProcessingException e) {
                log.warn("用户快照反序列化失败，将回源数据库: userId={}", userId, e);
            }
        }
        return new SessionSnapshot(token, user);
    }

    /**
     * 会话读取结果（令牌白名单值 + 已解析的用户快照）。
     *
     * @param token 白名单中的令牌；未登录/已注销为 null
     * @param user  用户快照；缓存未命中或解析失败为 null
     */
    public record SessionSnapshot(String token, LoginUser user) {
    }

    /**
     * 删除单个用户快照（角色/权限变更或用户变更后调用）。
     *
     * @param userId 用户ID
     */
    public void evictUser(Long userId) {
        if (userId == null) {
            return;
        }
        boolean deleted = redissonClient.getBucket(cacheKey(userId)).delete();
        log.info("清理用户权限缓存: userId={}, deleted={}", userId, deleted);
    }

    /**
     * 批量删除用户快照。
     *
     * <p>用 {@code RKeys.delete(keys...)} 一次往返删除全部：权限变更可能影响成百上千个用户，
     * 逐个 DEL 会放大成 N 次 RTT（这是"改一个权限卡一下"的常见来源）。</p>
     *
     * @param userIds 用户 ID 集合
     */
    public void evictUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        String[] keys = userIds.stream().filter(Objects::nonNull).map(this::cacheKey).toArray(String[]::new);
        if (keys.length == 0) {
            return;
        }
        long deleted = redissonClient.getKeys().delete(keys);
        log.info("批量清理用户权限缓存: 目标 {} 个, 实际删除 {} 个", keys.length, deleted);
    }

    /** 拼接缓存 key */
    private String cacheKey(Long userId) {
        return properties.getCache().getUserPermKeyPrefix() + userId;
    }
}
