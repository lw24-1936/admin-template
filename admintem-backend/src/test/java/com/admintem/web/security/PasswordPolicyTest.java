package com.admintem.web.security;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 密码策略测试。
 *
 * <p>锁住三件事：编码强度跟随配置、旧散列能被识别为"需要升级"、复杂度校验默认关闭。</p>
 */
class PasswordPolicyTest {

    private AdmintemProperties properties;
    private PasswordPolicy policy;

    @BeforeEach
    void setUp() {
        properties = new AdmintemProperties();
        policy = new PasswordPolicy(properties);
    }

    @Test
    @DisplayName("默认强度 10：编码结果包含 $2a$10$")
    void encodeUsesConfiguredStrength() {
        String hash = policy.encode("12345678");
        assertTrue(hash.startsWith("$2a$10$"), "默认应为代价因子 10，实际: " + hash);
    }

    @Test
    @DisplayName("提高强度后编码结果随之变化（$2a$12$）")
    void encodeFollowsRaisedStrength() {
        properties.getAuth().getPassword().setBcryptStrength(12);
        String hash = policy.encode("12345678");
        assertTrue(hash.startsWith("$2a$12$"), "应为代价因子 12，实际: " + hash);
    }

    @Test
    @DisplayName("强度越界时收敛到边界（不抛异常、不产生非法编码器）")
    void strengthIsClamped() {
        properties.getAuth().getPassword().setBcryptStrength(99);
        assertEquals(31, policy.currentStrength());
        properties.getAuth().getPassword().setBcryptStrength(1);
        assertEquals(4, policy.currentStrength());
    }

    @Test
    @DisplayName("旧散列（代价因子更低）被判定为需要升级")
    void detectsLaggingHash() {
        properties.getAuth().getPassword().setBcryptStrength(12);
        String oldHash = "$2a$10$abcdefghijklmnopqrstuv";
        assertTrue(policy.needsRehash(oldHash));
    }

    @Test
    @DisplayName("已达标散列 / 非 BCrypt / null 都不需要升级")
    void skipsWhenNothingToDo() {
        properties.getAuth().getPassword().setBcryptStrength(10);
        assertFalse(policy.needsRehash("$2a$10$abcdefghijklmnopqrstuv"));
        assertFalse(policy.needsRehash("$2a$12$abcdefghijklmnopqrstuv"));
        assertFalse(policy.needsRehash("plain-text"));
        assertFalse(policy.needsRehash(null));
    }

    @Test
    @DisplayName("关闭 rehash 开关后不再判定需要升级")
    void rehashSwitchRespected() {
        properties.getAuth().getPassword().setBcryptStrength(12);
        properties.getAuth().getPassword().setRehashOnLogin(false);
        assertFalse(policy.needsRehash("$2a$10$abcdefghijklmnopqrstuv"));
    }

    @Test
    @DisplayName("复杂度默认关闭：短密码通过")
    void complexityDisabledByDefault() {
        assertDoesNotThrow(() -> policy.validateComplexity("123456"));
    }

    @Test
    @DisplayName("开启复杂度后：纯数字 8 位被拒，字母+数字 8 位通过")
    void complexityEnabledEnforcesRule() {
        properties.getAuth().getPassword().setComplexityEnabled(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> policy.validateComplexity("12345678"));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), e.getCode());
        assertTrue(e.getMessage().contains("8 位"), "提示应说明规则，实际: " + e.getMessage());

        assertDoesNotThrow(() -> policy.validateComplexity("abc12345"));
    }

    @Test
    @DisplayName("复杂度校验开启后，null 不报错（由 @NotBlank 负责提示）")
    void complexityIgnoresNull() {
        properties.getAuth().getPassword().setComplexityEnabled(true);
        assertDoesNotThrow(() -> policy.validateComplexity(null));
    }
}
