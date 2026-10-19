package com.admintem.web.config.mybatis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SqlSlowInterceptor} 慢 SQL 阈值分级单元测试。
 *
 * <p>分级规则是"最长包含匹配，未命中回落默认阈值"，且日志会打印实际生效的阈值。
 * 这里把匹配规则固化下来，避免后续调整导致"热路径收紧失效"或"把重查询误判为慢"。</p>
 */
@DisplayName("慢 SQL 阈值分级")
class SqlSlowThresholdTest {

    private static final String USER_MAPPER = "com.admintem.web.mapper.SysAuthUserMapper";

    /** 与 application.yml 中的示例配置保持同样形态（Mapper 名 + 方法名两级） */
    private SqlSlowInterceptor interceptor() {
        Map<String, Long> byMapper = new LinkedHashMap<>();
        byMapper.put("SysAuthUserMapper.selectByUserName", 1000L);
        byMapper.put("SysAuthUserMapper.selectById", 1000L);
        byMapper.put("SysAuthUserMapper", 1500L);
        return new SqlSlowInterceptor(2000L, byMapper);
    }

    @Test
    @DisplayName("命中方法级规则时使用更严格的阈值")
    void usesMethodLevelThreshold() {
        assertThat(interceptor().resolveThreshold(USER_MAPPER + ".selectByUserName")).isEqualTo(1000L);
        assertThat(interceptor().resolveThreshold(USER_MAPPER + ".selectById")).isEqualTo(1000L);
    }

    @Test
    @DisplayName("未命中方法级但命中 Mapper 级时使用 Mapper 阈值")
    void usesMapperLevelThreshold() {
        assertThat(interceptor().resolveThreshold(USER_MAPPER + ".paginQuery")).isEqualTo(1500L);
        assertThat(interceptor().resolveThreshold(USER_MAPPER + ".deleteById")).isEqualTo(1500L);
    }

    @Test
    @DisplayName("最长匹配优先：同时命中方法与 Mapper 时取更长（更具体）的 key")
    void longestKeyWins() {
        // selectByUserName 与 SysAuthUserMapper 都能匹配，应取前者（1000 而非 1500）
        assertThat(interceptor().resolveThreshold(USER_MAPPER + ".selectByUserName")).isEqualTo(1000L);
    }

    @Test
    @DisplayName("完全未命中时回落默认阈值")
    void fallsBackToDefault() {
        SqlSlowInterceptor it = interceptor();
        assertThat(it.resolveThreshold("com.admintem.web.mapper.SysAuthRoleMapper.selectRolePage")).isEqualTo(2000L);
        assertThat(it.resolveThreshold("unknown")).isEqualTo(2000L);
    }

    @Test
    @DisplayName("mapperId 为 null 时回落默认阈值，不抛异常")
    void handlesNullMapperId() {
        assertThat(interceptor().resolveThreshold(null)).isEqualTo(2000L);
    }

    @Test
    @DisplayName("未配置分级阈值时一律使用默认阈值")
    void usesDefaultWhenNoPerMapperConfig() {
        SqlSlowInterceptor it = new SqlSlowInterceptor(2000L);
        assertThat(it.resolveThreshold(USER_MAPPER + ".selectByUserName")).isEqualTo(2000L);
        assertThat(it.resolveThreshold(USER_MAPPER + ".selectById")).isEqualTo(2000L);
    }

    @Test
    @DisplayName("空 Map 传给构造器安全（等价于未配置）")
    void handlesEmptyConfigMap() {
        SqlSlowInterceptor it = new SqlSlowInterceptor(2000L, Map.of());
        assertThat(it.resolveThreshold(USER_MAPPER + ".paginQuery")).isEqualTo(2000L);
    }

    @Test
    @DisplayName("配置中的 null key/value 被忽略，不参与匹配也不抛异常")
    void ignoresNullEntries() {
        Map<String, Long> byMapper = new LinkedHashMap<>();
        byMapper.put("SysAuthUserMapper.paginQuery", 800L);
        byMapper.put(null, 500L);
        byMapper.put("SysAuthRoleMapper", null);

        SqlSlowInterceptor it = new SqlSlowInterceptor(2000L, byMapper);
        assertThat(it.resolveThreshold(USER_MAPPER + ".paginQuery")).isEqualTo(800L);
        assertThat(it.resolveThreshold("com.admintem.web.mapper.SysAuthRoleMapper.selectRolePage")).isEqualTo(2000L);
    }
}
