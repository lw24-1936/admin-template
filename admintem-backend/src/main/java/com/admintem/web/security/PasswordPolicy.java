package com.admintem.web.security;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 密码策略：编码强度、登录时自动升级、可选复杂度校验。
 *
 * <p><b>为什么单独抽出来：</b>这三件事分散在"注册 / 新增用户 / 重置密码 / 修改本人密码"
 * 四条路径上，各写一遍就会漂移（例如重置密码忘了校验复杂度）。集中后：</p>
 * <ul>
 *   <li>编码统一走 {@link #encode(String)}，强度由配置决定；</li>
 *   <li>{@link #needsRehash(String)} 供登录成功后判断是否需要把旧散列升级；
 *       BCrypt 的代价因子写死在散列里，提高配置值只影响新密码，老用户需要"登录时升级"；</li>
 *   <li>{@link #validateComplexity(String)} 默认关闭，避免影响既有演示账号与接口回归。</li>
 * </ul>
 *
 * <p>安全说明：本类不做任何日志输出（密码/散列绝不进日志），错误提示不包含用户输入内容。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordPolicy {

    private final AdmintemProperties properties;

    /** BCrypt 代价因子合法范围（Spring 的实现允许 4~31） */
    private static final int MIN_STRENGTH = 4;
    private static final int MAX_STRENGTH = 31;

    /**
     * 编码密码（强度取配置值）。
     *
     * @param rawPassword 明文
     * @return BCrypt 散列
     */
    public String encode(String rawPassword) {
        return encoder().encode(rawPassword);
    }

    /**
     * 判断已有散列是否需要按当前配置升级。
     *
     * <p>只处理 BCrypt 散列（形如 {@code $2a$10$...}）：解析其中的代价因子与当前配置比较；
     * 非 BCrypt 或格式异常时返回 false（不动它，避免把兼容性问题变成登录失败）。</p>
     *
     * @param encodedPassword 库中的散列
     * @return true 表示登录成功后应重新编码并落库
     */
    public boolean needsRehash(String encodedPassword) {
        if (!properties.getAuth().getPassword().isRehashOnLogin()) {
            return false;
        }
        if (encodedPassword == null || !encodedPassword.startsWith("$2")) {
            return false;
        }
        // 形如 $2a$10$xxxx：第 3 段是代价因子
        String[] parts = encodedPassword.split("\\$");
        if (parts.length < 4) {
            return false;
        }
        try {
            int cost = Integer.parseInt(parts[2]);
            return cost < currentStrength();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 复杂度校验（仅当配置开启时生效）。
     *
     * @param rawPassword 明文
     * @throws BusinessException 不满足复杂度要求（400）
     */
    public void validateComplexity(String rawPassword) {
        AdmintemProperties.Auth.Password cfg = properties.getAuth().getPassword();
        if (!cfg.isComplexityEnabled() || rawPassword == null) {
            return;
        }
        if (!Pattern.compile(cfg.getComplexityRegex()).matcher(rawPassword).matches()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, cfg.getComplexityMessage());
        }
    }

    /**
     * 当前配置的代价因子。
     *
     * @return 合法范围内的强度值（配置越界时收敛到边界并告警）
     */
    public int currentStrength() {
        int strength = properties.getAuth().getPassword().getBcryptStrength();
        if (strength < MIN_STRENGTH || strength > MAX_STRENGTH) {
            log.warn("admintem.auth.password.bcrypt-strength={} 越界（{}~{}），已收敛到边界值",
                    strength, MIN_STRENGTH, MAX_STRENGTH);
            return strength < MIN_STRENGTH ? MIN_STRENGTH : MAX_STRENGTH;
        }
        return strength;
    }

    /** 按当前强度构造编码器（BCrypt 编码器不可变，这里按需创建） */
    private BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder(currentStrength());
    }
}
