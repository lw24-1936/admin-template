package com.admintem.web.service.impl;

import com.admintem.web.entity.SysAuthUser;
import com.admintem.web.service.PasswordResetNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 本地/演示环境的验证码发送：写入服务端日志。
 *
 * <p>只适合本地开发；生产请设置 {@code admintem.auth.password-reset.mode=smtp}。
 * 日志包含验证码，必须限制日志访问权限。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "admintem.auth.password-reset.mode", havingValue = "log", matchIfMissing = true)
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {

    @Override
    public void send(SysAuthUser user, String code) {
        log.warn("密码重置验证码（log 模式，仅本地开发）: userName={}, code={}", user.getUserName(), code);
    }
}
