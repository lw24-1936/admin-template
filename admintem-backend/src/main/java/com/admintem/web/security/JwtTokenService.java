package com.admintem.web.security;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.constant.SecurityConstants;
import com.admintem.web.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT 令牌服务：签发、解析、Redis 白名单校验与注销。
 *
 * <p>设计要点：
 * <ul>
 *   <li>令牌载荷仅存放 {@code subject=userName}、{@code userId}、{@code remember}、签发/过期时间，
 *       保持体积最小（避免旧项目曾遇到的请求头过大问题）；</li>
 *   <li>登录成功后，将令牌写入 Redis 白名单（key = prefix + userName，TTL 与令牌一致）；
 *       每次请求校验"签名合法 + 与白名单一致"，实现服务端可强制下线（注销/改密/禁用即删除白名单）；</li>
 *   <li>同一账号仅保留最新令牌（与旧项目单 key 语义一致，简单可预期）。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class JwtTokenService {

    private final AdmintemProperties properties;
    /** Redisson 客户端（白名单读写） */
    private final RedissonClient redissonClient;

    /**
     * 缓存的签名密钥。
     *
     * <p>构造时算一次即可：此前每次 {@code parseToken}/{@code createToken} 都重新
     * {@code getBytes} + {@code Keys.hmacShaKeyFor}，等于每个已登录请求多一次无谓分配。
     * 密钥来自配置，运行期不变，因此缓存是安全的。</p>
     */
    private final SecretKey signingKey;

    /** HS256 签名密钥的最小字节数（RFC 7518：HMAC-SHA256 密钥长度不得小于哈希输出长度 256 bit） */
    private static final int MIN_SECRET_BYTES = 32;

    /** 示例密钥的标记：application.yml 里的默认值带此串，生产环境遇到即拒绝启动 */
    private static final String DEFAULT_SECRET_MARKER = "change-me";

    /**
     * 构造器注入。
     *
     * <p>启动时做两道校验，都属于"宁可启动失败，也不要带病运行"：</p>
     * <ol>
     *   <li>密钥长度：未配置或不足 32 字节直接失败，避免直到用户登录才因 WeakKeyException 抛 500；</li>
     *   <li>生产默认密钥：profile 含 {@code pro} 且密钥仍是示例值（含 change-me 标记）时失败——
     *       这类"忘了换密钥"的配置事故不会自己暴露，只有令牌被伪造时才会发现。</li>
     * </ol>
     *
     * @param properties    业务配置（JWT 密钥与有效期）
     * @param redissonClient Redisson 客户端（白名单读写）
     * @param environment   运行环境（用于判断是否生产 profile）
     * @throws IllegalStateException 密钥缺失/过短，或生产环境仍在使用示例密钥
     */
    public JwtTokenService(AdmintemProperties properties, RedissonClient redissonClient,
                           Environment environment) {
        this.properties = properties;
        this.redissonClient = redissonClient;
        String secret = properties.getJwt().getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "admintem.jwt.secret 未配置或长度不足 " + MIN_SECRET_BYTES + " 字节，HS256 签名密钥非法");
        }
        // 生产环境拒绝示例密钥：示例值里带 change-me 标记（见 application.yml 注释）
        boolean proProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.toLowerCase().contains("pro"));
        if (proProfile && properties.getJwt().isFailOnDefaultSecret()
                && secret.toLowerCase().contains(DEFAULT_SECRET_MARKER)) {
            throw new IllegalStateException(
                    "生产环境（profile 含 pro）禁止使用示例 JWT 密钥，请通过环境变量 ADMINTEM_JWT_SECRET 注入强随机串");
        }
        // 启动时算一次并缓存（见字段注释：避免每请求重建密钥对象）
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发令牌并将白名单写入 Redis。
     *
     * @param userId   用户ID
     * @param userName 登录账号
     * @param remember 是否记住我（决定有效期）
     * @return JWT 字符串（不含 "LAUNCH " 前缀）
     */
    public String createToken(Long userId, String userName, boolean remember) {
        return createToken(userId, userName, remember, null);
    }

    /**
     * 签发令牌并将白名单写入 Redis（可选设备维度）。
     *
     * @param deviceId 设备 ID；为空时沿用旧版单账号单令牌 key
     */
    public String createToken(Long userId, String userName, boolean remember, String deviceId) {
        Date now = new Date();
        long ttlMillis = ttlMillis(remember);
        Date expireAt = new Date(now.getTime() + ttlMillis);
        String normalizedDeviceId = normalizeDeviceId(deviceId);

        // 组装 JWT：subject = 账号，附带 userId、remember 与可选设备 ID
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .subject(userName)
                .claim(SecurityConstants.CLAIM_USER_ID, userId)
                .claim(SecurityConstants.CLAIM_REMEMBER, remember);
        if (normalizedDeviceId != null) {
            builder.claim(SecurityConstants.CLAIM_DEVICE_ID, normalizedDeviceId);
        }
        String token = builder.issuedAt(now)
                .expiration(expireAt)
                .signWith(signingKey)
                .compact();

        // 令牌白名单写入 Redis，TTL 与令牌自身有效期保持一致（单位：秒）
        String redisKey = tokenRedisKey(userName, normalizedDeviceId);
        long ttlSeconds = ttlMillis / 1000;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        bucket.set(token, ttlSeconds, TimeUnit.SECONDS);
        log.info("签发令牌成功: userId={}, userName={}, remember={}, deviceId={}, ttlSeconds={}",
                userId, userName, remember, normalizedDeviceId, ttlSeconds);
        return token;
    }

    /**
     * 解析并校验令牌签名/有效期。
     *
     * @param token 原始 JWT
     * @return Claims
     * @throws BusinessException 令牌非法/过期（code=401）
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
    }

    /**
     * 注销：删除账号的令牌白名单（服务端立即失效）。
     *
     * @param userName 账号
     */
    public void revokeToken(String userName) {
        boolean deleted = redissonClient.getBucket(tokenRedisKey(userName)).delete();
        long expired = redissonClient.getKeys().deleteByPattern(
                properties.getJwt().getRedisKeyPrefix() + userName + ":*");
        log.info("注销令牌: userName={}, legacyDeleted={}, deviceDeleted={}", userName, deleted, expired);
    }

    /**
     * 注销指定设备的令牌（用于按设备强制下线）。
     */
    public void revokeToken(String userName, String deviceId) {
        String normalizedDeviceId = normalizeDeviceId(deviceId);
        if (normalizedDeviceId == null) {
            revokeToken(userName);
            return;
        }
        boolean deleted = redissonClient.getBucket(tokenRedisKey(userName, normalizedDeviceId)).delete();
        log.info("注销设备令牌: userName={}, deviceId={}, deleted={}", userName, normalizedDeviceId, deleted);
    }

    /* ------------------------------ 私有方法 ------------------------------ */

    /** 计算令牌有效期（毫秒） */
    private long ttlMillis(boolean remember) {
        if (remember) {
            return properties.getJwt().getRememberExpireDays() * 24 * 3600 * 1000L;
        }
        return properties.getJwt().getExpireMinutes() * 60 * 1000L;
    }


    /** Redis 白名单 key（旧版单账号单令牌） */
    private String tokenRedisKey(String userName) {
        return properties.getJwt().getRedisKeyPrefix() + userName;
    }

    /** Redis 白名单 key（设备维度） */
    private String tokenRedisKey(String userName, String deviceId) {
        return deviceId == null ? tokenRedisKey(userName)
                : properties.getJwt().getRedisKeyPrefix() + userName + ":" + deviceId;
    }

    /** 只接受安全字符，避免 Redis key 注入/超长；非法值回落到旧版单会话。 */
    private String normalizeDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }
        String value = deviceId.trim();
        return value.matches("[A-Za-z0-9_-]{1,64}") ? value : null;
    }
}
