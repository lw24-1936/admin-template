package com.admintem.web.security;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.admintem.web.config.AdmintemProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBuckets;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会话缓存读取测试。
 *
 * <p>锁住两件事：</p>
 * <ol>
 *   <li><b>一次往返</b>：令牌白名单与用户快照必须用一次 {@code RBuckets.get(keys...)}
 *       （底层 MGET）取回，而不是两次 GET——这是"每个已登录请求 2 个 RTT"的修复点；</li>
 *   <li><b>批量清理</b>：批量失效只调一次 {@code RKeys.delete(keys...)}，
 *       不随用户数线性增加往返。</li>
 * </ol>
 */
class RbacCacheServiceTest {

    private AdmintemProperties properties;
    private RedissonClient redisson;
    private RBuckets buckets;
    private RKeys keys;
    private RbacCacheService service;
    /**
     * 与 Spring Boot 的 ObjectMapper 行为对齐：忽略未知属性。
     * LoginUser 实现了 UserDetails，序列化会多出 enabled/accountNonExpired 等派生属性，
     * 严格模式会被判为未知字段，导致快照反序列化失败（真实环境不会）。
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @BeforeEach
    void setUp() {
        properties = new AdmintemProperties();
        redisson = mock(RedissonClient.class);
        buckets = mock(RBuckets.class);
        keys = mock(RKeys.class);
        when(redisson.getBuckets()).thenReturn(buckets);
        when(redisson.getKeys()).thenReturn(keys);
        service = new RbacCacheService(redisson, objectMapper, properties);
    }

    @Test
    @DisplayName("readSession：一次 MGET 同时取回令牌与快照")
    void readsTokenAndSnapshotInOneRoundTrip() throws Exception {
        String tokenKey = properties.getJwt().getRedisKeyPrefix() + "admin";
        String permKey = properties.getCache().getUserPermKeyPrefix() + "1";
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setUserName("admin");
        user.setStatus(0);

        Map<String, String> values = new HashMap<>();
        values.put(tokenKey, "jwt-token-value");
        values.put(permKey, objectMapper.writeValueAsString(user));
        doReturn(values).when(buckets).get(any(String[].class));

        RbacCacheService.SessionSnapshot snapshot = service.readSession("admin", 1L);

        assertEquals("jwt-token-value", snapshot.token());
        assertNotNull(snapshot.user());
        assertEquals("admin", snapshot.user().getUserName());
        // 关键断言：只有一次读取（MGET），不是两次 GET
        verify(buckets, times(1)).get(any(String[].class));
    }

    @Test
    @DisplayName("readSession：两个 key 都不存在时返回空结果（未登录）")
    void returnsEmptyWhenKeysMissing() {
        doReturn(new HashMap<>()).when(buckets).get(any(String[].class));

        RbacCacheService.SessionSnapshot snapshot = service.readSession("ghost", 99L);

        assertNull(snapshot.token());
        assertNull(snapshot.user());
    }

    @Test
    @DisplayName("readSession：快照 JSON 损坏时令牌仍可用，用户为 null（触发回源重建）")
    void keepsTokenWhenSnapshotCorrupted() {
        String tokenKey = properties.getJwt().getRedisKeyPrefix() + "admin";
        String permKey = properties.getCache().getUserPermKeyPrefix() + "1";
        Map<String, String> values = new HashMap<>();
        values.put(tokenKey, "jwt-token-value");
        values.put(permKey, "{not-a-json");
        doReturn(values).when(buckets).get(any(String[].class));

        RbacCacheService.SessionSnapshot snapshot = service.readSession("admin", 1L);

        assertEquals("jwt-token-value", snapshot.token());
        assertNull(snapshot.user());
    }

    @Test
    @DisplayName("evictUsers：批量删除只调用一次 RKeys.delete")
    void batchEvictUsesSingleCall() {
        when(keys.delete(any(String[].class))).thenReturn(3L);

        service.evictUsers(List.of(1L, 2L, 3L));

        verify(keys, times(1)).delete(any(String[].class));
        verify(redisson, times(0)).getBucket(anyString());
    }

    @Test
    @DisplayName("evictUsers：空集合不触发任何 Redis 调用")
    void emptyEvictIsNoop() {
        service.evictUsers(List.of());

        verify(keys, times(0)).delete(any(String[].class));
    }
}
