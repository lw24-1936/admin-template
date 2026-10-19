package com.admintem.web.config.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redisson 客户端连接参数（对应各环境 yml 中顶层 {@code redisson.*}）。
 *
 * <p>由 {@link RedissonConfig} 通过 {@code @EnableConfigurationProperties} 注册并构建
 * {@code RedissonClient}；密钥、地址等配置随 Maven Profile 切换（dev/test/pre/pro）。</p>
 */
@Data
@ConfigurationProperties(prefix = "redisson")
public class RedissonProperties {

    /** 单机连接地址，形如 redis://host:port（支持 redis:// 与 rediss://） */
    private String address;

    /** 连接密码（无密码环境可留空） */
    private String password = "";

    /** 逻辑库编号（默认 0，与旧项目一致） */
    private int database = 0;

    /** 连接超时（毫秒） */
    private int connectTimeoutMs = 5000;

    /** 命令执行超时（毫秒） */
    private int timeoutMs = 5000;

    /** 连接池大小 */
    private int connectionPoolSize = 16;

    /** 最小空闲连接 */
    private int minIdleSize = 2;
}
