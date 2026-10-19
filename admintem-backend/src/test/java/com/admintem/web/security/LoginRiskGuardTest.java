package com.admintem.web.security;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录风控守卫测试（账号维度 + IP 维度）。
 *
 * <p>这组测试锁定的是"什么时候该拦住、什么时候不该拦"：误拦会挡住正常用户，
 * 漏拦则风控形同虚设。因此对两个维度的阈值、开关与清理时机都给出显式断言。</p>
 *
 * <p>不连 Redis：用 Mockito 模拟 Redisson 的 bucket 与计数器，并用本地 Map 记录状态，
 * 使"计数 → 达阈值 → 置锁"的行为可被真实断言。</p>
 */
class LoginRiskGuardTest {

    private AdmintemProperties properties;
    private RedissonClient redissonClient;
    private LoginRiskGuard guard;

    /** 模拟的字符串桶（key -> 是否存在） */
    private final Set<String> buckets = new HashSet<>();
    /** 模拟的计数器（key -> 计数值） */
    private final Map<String, Long> counters = new HashMap<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        properties = new AdmintemProperties();
        redissonClient = mock(RedissonClient.class);
        buckets.clear();
        counters.clear();

        // 模拟 RBucket：isExists / set（void，须用 doAnswer）/ delete
        when(redissonClient.getBucket(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            RBucket<String> bucket = mock(RBucket.class);
            when(bucket.isExists()).thenAnswer(i -> buckets.contains(key));
            doAnswer(i -> {
                buckets.add(key);
                return null;
            }).when(bucket).set(anyString(), anyLong(), any(TimeUnit.class));
            // Redis 的 DEL 与类型无关：删除 key 时两类状态都要清掉，
            // 这样测试才与真实行为一致（否则会掩盖/伪造失败）
            when(bucket.delete()).thenAnswer(i -> {
                counters.remove(key);
                return buckets.remove(key);
            });
            return bucket;
        });

        // 模拟 RAtomicLong：incrementAndGet / expire / delete
        when(redissonClient.getAtomicLong(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            RAtomicLong counter = mock(RAtomicLong.class);
            when(counter.incrementAndGet()).thenAnswer(i -> counters.merge(key, 1L, Long::sum));
            // expire 在 Redisson 中重载了 Duration 与 Instant 两个版本，需显式指定类型
            when(counter.expire(any(Duration.class))).thenReturn(true);
            when(counter.delete()).thenAnswer(i -> {
                buckets.remove(key);
                return counters.remove(key) != null;
            });
            return counter;
        });

        guard = new LoginRiskGuard(properties, redissonClient);
    }

    @Test
    @DisplayName("账号锁定检查：未锁定时放行，锁定时抛 502")
    void checkAccountLocked() {
        assertDoesNotThrow(() -> guard.checkAccountLocked("admin"));

        buckets.add(properties.getAuth().getLockKeyPrefix() + "admin");
        BusinessException e = assertThrows(BusinessException.class, () -> guard.checkAccountLocked("admin"));
        assertEquals(ResultCode.ACCOUNT_LOCKED.getCode(), e.getCode());
        assertTrue(e.getMessage().contains("已锁定"));
    }

    @Test
    @DisplayName("账号维度：连续失败达到阈值即锁定账号并清空计数")
    void lockAccountAtThreshold() {
        properties.getAuth().setMaxLoginFailCount(3);

        guard.onLoginFailure("admin", "127.0.0.1");
        guard.onLoginFailure("admin", "127.0.0.1");
        assertFalse(buckets.contains(properties.getAuth().getLockKeyPrefix() + "admin"), "未达阈值不应锁定");

        guard.onLoginFailure("admin", "127.0.0.1");
        assertTrue(buckets.contains(properties.getAuth().getLockKeyPrefix() + "admin"), "达阈值应锁定账号");
        assertFalse(counters.containsKey(properties.getAuth().getFailCountKeyPrefix() + "admin"), "锁定后应清空失败计数");
    }

    @Test
    @DisplayName("IP 维度默认关闭：不计数、不限制（避免影响本地联调）")
    void ipDimensionDisabledByDefault() {
        assertFalse(properties.getAuth().isIpLimitEnabled());

        for (int i = 0; i < 100; i++) {
            guard.onLoginFailure("admin", "10.0.0.9");
        }

        assertFalse(counters.containsKey(properties.getAuth().getIpFailCountKeyPrefix() + "10.0.0.9"));
        assertDoesNotThrow(() -> guard.checkIpLimited("10.0.0.9", "admin"));
    }

    @Test
    @DisplayName("IP 维度开启：同一 IP 达到阈值即限制，并抛 511")
    void ipDimensionLocksAtThreshold() {
        properties.getAuth().setIpLimitEnabled(true);
        properties.getAuth().setIpMaxLoginFailCount(3);

        guard.checkIpLimited("10.0.0.9", "admin");
        guard.onLoginFailure("u1", "10.0.0.9");
        guard.onLoginFailure("u2", "10.0.0.9");
        guard.onLoginFailure("u3", "10.0.0.9");

        assertTrue(buckets.contains(properties.getAuth().getIpLockKeyPrefix() + "10.0.0.9"), "达阈值应限制该 IP");
        BusinessException e = assertThrows(BusinessException.class, () -> guard.checkIpLimited("10.0.0.9", "u4"));
        assertEquals(ResultCode.LOGIN_IP_LIMITED.getCode(), e.getCode());
        // 另一个 IP 不受影响：IP 维度只惩罚来源，不牵连他人
        assertDoesNotThrow(() -> guard.checkIpLimited("10.0.0.8", "u4"));
    }

    @Test
    @DisplayName("IP 无法识别（null）：跳过 IP 维度，避免误伤")
    void skipWhenIpUnknown() {
        properties.getAuth().setIpLimitEnabled(true);
        properties.getAuth().setIpMaxLoginFailCount(1);

        assertDoesNotThrow(() -> guard.checkIpLimited(null, "admin"));
        for (int i = 0; i < 5; i++) {
            guard.onLoginFailure("admin", null);
        }
        // 账号维度的计数仍然生效（不受 IP 缺失影响）
        assertTrue(buckets.contains(properties.getAuth().getLockKeyPrefix() + "admin"));
        verify(redissonClient, never()).getAtomicLong(eq(properties.getAuth().getIpFailCountKeyPrefix() + "null"));
    }

    @Test
    @DisplayName("登录成功：清理账号计数与锁定标记，并清空该 IP 的失败计数")
    void clearOnSuccess() {
        properties.getAuth().setIpLimitEnabled(true);
        properties.getAuth().setMaxLoginFailCount(5);

        guard.onLoginFailure("admin", "10.0.0.9");
        guard.onLoginFailure("admin", "10.0.0.9");
        assertTrue(counters.containsKey(properties.getAuth().getFailCountKeyPrefix() + "admin"));

        guard.onLoginSuccess("admin", "10.0.0.9");

        assertFalse(counters.containsKey(properties.getAuth().getFailCountKeyPrefix() + "admin"));
        assertFalse(counters.containsKey(properties.getAuth().getIpFailCountKeyPrefix() + "10.0.0.9"));
        assertDoesNotThrow(() -> guard.checkAccountLocked("admin"));
    }

    @Test
    @DisplayName("已被限制的 IP：用正确密码登录也不会提前解封")
    void lockedIpNotClearedBySuccess() {
        properties.getAuth().setIpLimitEnabled(true);
        properties.getAuth().setIpMaxLoginFailCount(2);
        guard.onLoginFailure("u1", "10.0.0.9");
        guard.onLoginFailure("u2", "10.0.0.9");
        assertTrue(buckets.contains(properties.getAuth().getIpLockKeyPrefix() + "10.0.0.9"));

        // 模拟"成功登录"路径调用（真实链路中该调用在 checkIpLimited 之后，这里直接验证语义）
        guard.onLoginSuccess("u3", "10.0.0.9");

        assertTrue(
                buckets.contains(properties.getAuth().getIpLockKeyPrefix() + "10.0.0.9"),
                "限制期内不应因一次成功登录而解封");
    }
}
