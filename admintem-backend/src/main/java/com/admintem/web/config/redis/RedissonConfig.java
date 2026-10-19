package com.admintem.web.config.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson 客户端配置。
 *
 * <p>依据各环境 yml 的 {@code redisson.*}（{@link RedissonProperties}）
 * 显式构建单机模式的 {@code RedissonClient}，供以下场景使用：</p>
 * <ul>
 *   <li>JWT 令牌白名单（服务端强制下线）；</li>
 *   <li>登录用户权限快照缓存（按钮级权限判定）；</li>
 *   <li>登录失败计数 / 账号锁定（RAtomicLong + RBucket）。</li>
 * </ul>
 *
 * <p>说明：不使用 spring-boot-starter-data-redis（Lettuce），Redis 访问统一走 Redisson，
 * 与旧项目“Redisson + 单机 Redis”的成熟方案保持一致。</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RedissonProperties.class)
public class RedissonConfig {

    /**
     * 构建单机 RedissonClient（随容器关闭自动 shutdown）。
     *
     * @param props 连接参数
     * @return RedissonClient
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedissonProperties props) {
        Config config = new Config();
        SingleServerConfig single = config.useSingleServer()
                .setAddress(props.getAddress())
                .setDatabase(props.getDatabase())
                .setConnectTimeout(props.getConnectTimeoutMs())
                .setTimeout(props.getTimeoutMs())
                .setConnectionPoolSize(props.getConnectionPoolSize())
                .setConnectionMinimumIdleSize(props.getMinIdleSize());
        if (StringUtils.hasText(props.getPassword())) {
            single.setPassword(props.getPassword());
        }
        RedissonClient client = Redisson.create(config);
        log.info("RedissonClient 初始化成功: address={}, database={}", props.getAddress(), props.getDatabase());
        return client;
    }
}
