package com.admintem.web.config.mybatis;

import com.admintem.web.config.AdmintemProperties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 慢 SQL 统计拦截器（参考 config/mybatis/SqlStatementInterceptor 的分级思路）。
 *
 * <p>以 MyBatis Executor 插件方式统计 update/query 执行耗时，超过阈值输出 WARN 到日志器
 * {@code sys-slow-sql}（独立文件 logs/sql/admintem-slow.log），方便快速定位慢查询。</p>
 *
 * <p>阈值支持**按语句分级**：默认阈值 {@code admintem.sql.slow-ms}（默认 2000ms），
 * 并可通过 {@code admintem.sql.slow-ms-by-mapper} 为特定 mapper（或 Mapper 方法前缀）
 * 设置更严格的阈值，例如把登录/会话这类热路径收紧到 300ms，把报表类放宽到 5000ms。
 * 匹配规则：取"最长匹配前缀"对应的阈值，未命中则用默认阈值。</p>
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class,
                        ResultHandler.class, org.apache.ibatis.cache.CacheKey.class, BoundSql.class})
})
public class SqlSlowInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger("sys-slow-sql");

    /** 默认慢 SQL 阈值（毫秒） */
    private final long slowMs;

    /**
     * 按 mapperId 前缀区分的阈值（毫秒）；按 key 长度倒序排列，便于取最长匹配
     * 例如 {@code SysAuthUserMapper.select} -> 300
     */
    private final Map<String, Long> slowMsByMapper;

    public SqlSlowInterceptor(long slowMs) {
        this(slowMs, Collections.emptyMap());
    }

    /**
     * @param slowMs          默认慢 SQL 阈值（毫秒）
     * @param slowMsByMapper  按 mapperId 前缀区分的阈值（可为空）
     */
    public SqlSlowInterceptor(long slowMs, Map<String, Long> slowMsByMapper) {
        this.slowMs = slowMs;
        Map<String, Long> ordered = new LinkedHashMap<>();
        if (slowMsByMapper != null) {
            slowMsByMapper.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getValue() != null)
                    // 长 key 优先，保证"最长前缀匹配"
                    .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
                    .forEach(e -> ordered.put(e.getKey(), e.getValue()));
        }
        this.slowMsByMapper = Collections.unmodifiableMap(ordered);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            String mapperId = resolveMapperId(invocation);
            long threshold = resolveThreshold(mapperId);
            if (cost > threshold) {
                log.warn("慢SQL(>{}ms): 耗时={}ms, mapperId={}", threshold, cost, mapperId);
            }
        }
    }

    /** 取 MappedStatement.getId() 便于定位具体 SQL */
    private String resolveMapperId(Invocation invocation) {
        Object[] args = invocation.getArgs();
        if (args != null && args.length > 0 && args[0] instanceof MappedStatement ms) {
            return ms.getId();
        }
        return "unknown";
    }

    /**
     * 解析该 mapperId 适用的阈值：优先最长前缀匹配，未命中用默认值。
     *
     * <p>包内可见（非 private）：便于单元测试直接验证分级匹配规则，
     * 见 {@code com.admintem.web.config.mybatis.SqlSlowThresholdTest}。</p>
     *
     * @param mapperId MappedStatement id（含全限定 Mapper 名与方法名）
     * @return 阈值（毫秒）
     */
    long resolveThreshold(String mapperId) {
        if (slowMsByMapper.isEmpty() || mapperId == null) {
            return slowMs;
        }
        for (Map.Entry<String, Long> entry : slowMsByMapper.entrySet()) {
            if (mapperId.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return slowMs;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 参数已通过构造器注入，无需配置项
    }
}
