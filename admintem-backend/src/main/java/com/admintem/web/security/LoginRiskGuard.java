package com.admintem.web.security;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 登录风控守卫：集中处理"连续登录失败"的账号维度与 IP 维度限制。
 *
 * <p>为什么独立成一个组件：这套逻辑原本内联在 {@code AuthServiceImpl} 里，
 * 加入 IP 维度后会有 4 组状态（账号计数/账号锁、IP 计数/IP 锁）与多处日志交织，
 * 混在登录主流程里既难读也难测。抽出来后：</p>
 * <ul>
 *   <li>登录主流程只表达"检查 → 认证 → 记失败/清失败"三步意图；</li>
 *   <li>风控策略（阈值、时长、key 前缀、告警文案）集中一处，便于按环境调整；</li>
 *   <li>可以用 Mockito 直接对 Redisson 断言，不必拉起整个认证链路。</li>
 * </ul>
 *
 * <p>两个维度的分工：</p>
 * <table border="1">
 *   <caption>维度对比</caption>
 *   <tr><th>维度</th><th>阈值</th><th>作用</th><th>默认</th></tr>
 *   <tr><td>账号</td><td>{@code admintem.auth.max-login-fail-count}（默认 5）</td>
 *       <td>锁账号，防止针对某个账号的猜密码</td><td>开启</td></tr>
 *   <tr><td>IP</td><td>{@code admintem.auth.ip-max-login-fail-count}（默认 20）</td>
 *       <td>限制来源网络，防止"撞库"（换账号名继续试）</td><td>关闭</td></tr>
 * </table>
 *
 * <p>为什么 IP 维度默认关闭：本地联调与演示常在同一个 IP 上反复输错密码，
 * 开启会把开发者自己挡住；生产建议开启（见《04-部署与操作手册》配置项说明）。</p>
 *
 * <p>安全说明：日志只记录账号与 IP，**绝不记录密码**；触发限制时使用 WARN 级别，
 * 便于运维按关键字告警（"登录风控"）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRiskGuard {

    /** 日志/告警统一前缀，便于日志系统按关键字抓取 */
    private static final String ALERT_TAG = "登录风控";

    private final AdmintemProperties properties;
    private final RedissonClient redissonClient;

    /**
     * 检查账号是否处于锁定状态。
     *
     * @param username 登录账号
     * @throws BusinessException 账号被锁定时抛出（业务码 502）
     */
    public void checkAccountLocked(String username) {
        String lockKey = properties.getAuth().getLockKeyPrefix() + username;
        if (redissonClient.getBucket(lockKey).isExists()) {
            log.warn("[{}] 账号处于锁定状态，登录被拒绝: username={}", ALERT_TAG, username);
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED,
                    "账号已锁定，请 " + properties.getAuth().getLoginLockMinutes() + " 分钟后再试");
        }
    }

    /**
     * 检查来源 IP 是否处于受限状态（仅当启用 IP 维度风控时生效）。
     *
     * @param ip       来源 IP（为 null 时跳过：无法识别来源就不做 IP 维度限制，避免误伤）
     * @param username 当前尝试的账号（仅用于日志）
     * @throws BusinessException IP 受限时抛出（业务码 511）
     */
    public void checkIpLimited(String ip, String username) {
        if (!properties.getAuth().isIpLimitEnabled() || ip == null) {
            return;
        }
        String lockKey = properties.getAuth().getIpLockKeyPrefix() + ip;
        if (redissonClient.getBucket(lockKey).isExists()) {
            log.warn("[{}] 来源IP处于限制状态，登录被拒绝: ip={}, username={}", ALERT_TAG, ip, username);
            throw new BusinessException(ResultCode.LOGIN_IP_LIMITED,
                    "当前网络登录尝试过于频繁，请 " + properties.getAuth().getIpLockMinutes() + " 分钟后再试");
        }
    }

    /**
     * 记录一次登录失败：账号维度计数（达阈值锁账号）+ IP 维度计数（达阈值限 IP）。
     *
     * @param username 登录账号
     * @param ip       来源 IP（可为 null）
     */
    public void onLoginFailure(String username, String ip) {
        recordAccountFailure(username);
        recordIpFailure(ip, username);
    }

    /**
     * 登录成功后清理两个维度的失败计数与锁定标记。
     *
     * <p>注意清理失败计数要用 {@code getAtomicLong().delete()} 而不是
     * {@code getBucket().delete()}：计数 key 存的是数值类型，用同类型的 API 删除
     * 语义更明确（虽然 Redis 的 DEL 本身与类型无关，但混用会让人怀疑是否真的删掉了，
     * 也让单元测试难以忠实模拟）。</p>
     *
     * @param username 登录账号
     * @param ip       来源 IP（可为 null）
     */
    public void onLoginSuccess(String username, String ip) {
        redissonClient.getAtomicLong(properties.getAuth().getFailCountKeyPrefix() + username).delete();
        redissonClient.getBucket(properties.getAuth().getLockKeyPrefix() + username).delete();
        if (properties.getAuth().isIpLimitEnabled() && ip != null) {
            // 只清失败计数：若该 IP 已进入限制期，正确密码也不应提前解封
            redissonClient.getAtomicLong(properties.getAuth().getIpFailCountKeyPrefix() + ip).delete();
        }
    }

    /* ------------------------------ 内部实现 ------------------------------ */

    /** 账号维度：连续失败达到阈值则锁定账号 */
    private void recordAccountFailure(String username) {
        String failKey = properties.getAuth().getFailCountKeyPrefix() + username;
        RAtomicLong counter = redissonClient.getAtomicLong(failKey);
        long count = counter.incrementAndGet();
        // 计数 key 设置过期时间（防止长期占用内存）
        counter.expire(Duration.ofMinutes(properties.getAuth().getLoginLockMinutes()));
        int max = properties.getAuth().getMaxLoginFailCount();
        if (count >= max) {
            String lockKey = properties.getAuth().getLockKeyPrefix() + username;
            redissonClient.<String>getBucket(lockKey)
                    .set("1", properties.getAuth().getLoginLockMinutes(), TimeUnit.MINUTES);
            // 清理失败计数，避免解锁后仍残留
            counter.delete();
            log.warn("[{}] 账号连续登录失败达到 {} 次，已锁定 {} 分钟: username={}",
                    ALERT_TAG, max, properties.getAuth().getLoginLockMinutes(), username);
        }
    }

    /** IP 维度：同一来源连续失败达到阈值则临时限制该 IP */
    private void recordIpFailure(String ip, String username) {
        if (!properties.getAuth().isIpLimitEnabled() || ip == null) {
            return;
        }
        String failKey = properties.getAuth().getIpFailCountKeyPrefix() + ip;
        RAtomicLong counter = redissonClient.getAtomicLong(failKey);
        long count = counter.incrementAndGet();
        counter.expire(Duration.ofMinutes(properties.getAuth().getIpLockMinutes()));
        int max = properties.getAuth().getIpMaxLoginFailCount();
        if (count >= max) {
            String lockKey = properties.getAuth().getIpLockKeyPrefix() + ip;
            redissonClient.<String>getBucket(lockKey)
                    .set("1", properties.getAuth().getIpLockMinutes(), TimeUnit.MINUTES);
            counter.delete();
            // 这是典型的攻击特征：同一来源大量失败，且账号名可能在变化 —— 值得单独告警
            log.warn("[{}] 来源IP连续登录失败达到 {} 次，已限制 {} 分钟: ip={}, 最近尝试账号={}",
                    ALERT_TAG, max, properties.getAuth().getIpLockMinutes(), ip, username);
        }
    }
}
