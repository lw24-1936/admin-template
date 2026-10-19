package com.admintem.web.util.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SortWhitelist} 单元测试。
 *
 * <p>排序白名单是"防 SQL 注入"的关键约定：前端只能传逻辑字段名，
 * 服务端映射为列名后才拼入 ORDER BY。这里覆盖映射、方向、拒绝与边界。</p>
 */
@DisplayName("列表排序白名单")
class SortWhitelistTest {

    private static final Map<String, String> WHITELIST = Map.of(
            "userName", "user_name",
            "status", "status");

    @Test
    @DisplayName("白名单字段 + asc/desc 正确映射为列名与方向")
    void resolveMapsFieldAndDirection() {
        assertThat(SortWhitelist.resolve("userName", "asc", WHITELIST)).isEqualTo("user_name ASC");
        assertThat(SortWhitelist.resolve("userName", "desc", WHITELIST)).isEqualTo("user_name DESC");
        assertThat(SortWhitelist.resolve("status", "DESC", WHITELIST)).isEqualTo("status DESC");
    }

    @Test
    @DisplayName("方向缺省或非法时回落 ASC，不产生拼装歧义")
    void resolveFallsBackToAsc() {
        assertThat(SortWhitelist.resolve("userName", null, WHITELIST)).isEqualTo("user_name ASC");
        assertThat(SortWhitelist.resolve("userName", "", WHITELIST)).isEqualTo("user_name ASC");
        assertThat(SortWhitelist.resolve("userName", "sideways", WHITELIST)).isEqualTo("user_name ASC");
    }

    @Test
    @DisplayName("未登记字段一律拒绝（返回 null，由调用方回落默认排序）")
    void resolveRejectsUnknownField() {
        assertThat(SortWhitelist.resolve("password", "asc", WHITELIST)).isNull();
        assertThat(SortWhitelist.resolve("user_name", "asc", WHITELIST)).isNull(); // 真实列名不作为逻辑名
        assertThat(SortWhitelist.resolve("1;DROP TABLE sys_auth_user", "asc", WHITELIST)).isNull();
        assertThat(SortWhitelist.resolve("userName)", "asc", WHITELIST)).isNull();
    }

    @Test
    @DisplayName("空入参（字段为空/白名单为空）返回 null")
    void resolveHandlesBlankInput() {
        assertThat(SortWhitelist.resolve(null, "asc", WHITELIST)).isNull();
        assertThat(SortWhitelist.resolve("", "asc", WHITELIST)).isNull();
        assertThat(SortWhitelist.resolve("   ", "asc", WHITELIST)).isNull();
        assertThat(SortWhitelist.resolve("userName", "asc", Map.of())).isNull();
        assertThat(SortWhitelist.resolve("userName", "asc", null)).isNull();
    }

    @Test
    @DisplayName("字段名首尾空白被忽略")
    void resolveTrimsField() {
        assertThat(SortWhitelist.resolve("  userName  ", "asc", WHITELIST)).isEqualTo("user_name ASC");
    }

    @Test
    @DisplayName("accepted：用于区分“未传”与“传了非法值”")
    void acceptedDistinguishesAbsentFromInvalid() {
        assertThat(SortWhitelist.accepted("userName", WHITELIST)).isTrue();
        assertThat(SortWhitelist.accepted("  status ", WHITELIST)).isTrue();
        assertThat(SortWhitelist.accepted("password", WHITELIST)).isFalse();
        assertThat(SortWhitelist.accepted(null, WHITELIST)).isFalse();
        assertThat(SortWhitelist.accepted("", WHITELIST)).isFalse();
    }
}
