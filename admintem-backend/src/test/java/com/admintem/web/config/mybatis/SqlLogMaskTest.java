package com.admintem.web.config.mybatis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SqlLogInnerInterceptor} 脱敏规则单元测试。
 *
 * <p>全量 SQL 日志会把参数值写进文件，其中可能包含手机号、邮箱、证件号与口令。
 * 这里锁定脱敏口径，防止后续改动把敏感值重新写进日志（属安全回归）。</p>
 */
@DisplayName("SQL 日志脱敏")
class SqlLogMaskTest {

    /** 开启脱敏的拦截器实例（不参与 SQL 执行，仅验证脱敏函数） */
    private final SqlLogInnerInterceptor interceptor = new SqlLogInnerInterceptor(true);

    /** 关闭脱敏的拦截器实例 */
    private final SqlLogInnerInterceptor noMask = new SqlLogInnerInterceptor(false);

    @Test
    @DisplayName("口令/令牌/密钥类字段整体打码，不保留长度信息")
    void masksSecretsEntirely() {
        assertThat(interceptor.maskText("password", "PlainPassw0rd!")).isEqualTo("***");
        assertThat(interceptor.maskText("oldPassword", "abc123")).isEqualTo("***");
        assertThat(interceptor.maskText("pwd", "abc123")).isEqualTo("***");
        assertThat(interceptor.maskText("accessToken", "eyJhbGci")).isEqualTo("***");
        assertThat(interceptor.maskText("secret", "s3cr3t")).isEqualTo("***");
    }

    @Test
    @DisplayName("手机号保留首 3 位与后 4 位")
    void masksPhone() {
        assertThat(interceptor.maskText("phone", "13912345678")).isEqualTo("139****5678");
        assertThat(interceptor.maskText("mobile", "13800001111")).isEqualTo("138****1111");
        assertThat(interceptor.maskText("tel", "13700002222")).isEqualTo("137****2222");
    }

    @Test
    @DisplayName("邮箱保留首字母与域名")
    void masksEmail() {
        assertThat(interceptor.maskText("email", "zhangsan@example.com")).isEqualTo("z***@example.com");
        assertThat(interceptor.maskText("mail", "a@b.cn")).isEqualTo("a***@b.cn");
    }

    @Test
    @DisplayName("证件号保留前 6 位与后 4 位")
    void masksIdCard() {
        assertThat(interceptor.maskText("idCard", "110101199003071234")).isEqualTo("110101********1234");
        assertThat(interceptor.maskText("cardNo", "11010119900307123X")).isEqualTo("110101********123X");
    }

    @Test
    @DisplayName("无参数名（单值参数）时按值特征兜底脱敏")
    void masksByShapeWhenNameAbsent() {
        assertThat(interceptor.maskText(null, "13912345678")).isEqualTo("139****5678");
        assertThat(interceptor.maskText(null, "zhangsan@example.com")).isEqualTo("z***@example.com");
        assertThat(interceptor.maskText(null, "110101199003071234")).isEqualTo("110101********1234");
    }

    @Test
    @DisplayName("普通字段（账号/昵称/菜单名）保持原样，保证可排查性")
    void keepsNonSensitiveValuesReadable() {
        assertThat(interceptor.maskText("userName", "admin")).isEqualTo("admin");
        assertThat(interceptor.maskText("nickName", "超级管理员")).isEqualTo("超级管理员");
        assertThat(interceptor.maskText("permissionKey", "userInfo:userInfo")).isEqualTo("userInfo:userInfo");
        assertThat(interceptor.maskText("name", "工作台")).isEqualTo("工作台");
        // 字段名未命中关键字且不像敏感值时，不做兜底改写
        assertThat(interceptor.maskText("pageSize", "10")).isEqualTo("10");
    }

    @Test
    @DisplayName("字段名未命中但值是手机号形态时也会脱敏（防漏网）")
    void masksSensitiveShapeEvenWithoutKeywordName() {
        assertThat(interceptor.maskText("contact", "13912345678")).isEqualTo("139****5678");
    }

    @Test
    @DisplayName("关闭脱敏开关后原样返回（仅用于临时排查）")
    void skipsMaskingWhenDisabled() {
        assertThat(noMask.maskText("password", "PlainPassw0rd!")).isEqualTo("PlainPassw0rd!");
        assertThat(noMask.maskText("phone", "13912345678")).isEqualTo("13912345678");
    }

    @Test
    @DisplayName("空值不参与脱敏，避免空串被误写成掩码")
    void keepsEmptyValue() {
        assertThat(interceptor.maskText("phone", "")).isEmpty();
    }

    @Test
    @DisplayName("关键字匹配不区分大小写")
    void keywordMatchIsCaseInsensitive() {
        assertThat(interceptor.maskText("PassWord", "abc")).isEqualTo("***");
        assertThat(interceptor.maskText("PHONE", "13912345678")).isEqualTo("139****5678");
    }
}
