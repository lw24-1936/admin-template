package com.admintem.web.service.impl;

import com.admintem.web.entity.SysAuthUser;
import com.admintem.web.service.PasswordResetNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SMTP 验证码发送；要求用户配置邮箱。
 *
 * <p>启用方式：{@code admintem.auth.password-reset.mode=smtp} 并配置
 * {@code spring.mail.*}。邮箱为空时只记录脱敏告警，不把验证码写日志。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "admintem.auth.password-reset.mode", havingValue = "smtp")
public class SmtpPasswordResetNotifier implements PasswordResetNotifier {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Override
    public void send(SysAuthUser user, String code) {
        if (!StringUtils.hasText(user.getEmail())) {
            log.warn("密码重置：用户未配置邮箱，无法发送验证码: userName={}", user.getUserName());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("admintem 密码重置验证码");
        message.setText("你的密码重置验证码是：" + code + "，10 分钟内有效。如非本人操作请忽略。");
        mailSender.send(message);
        log.info("密码重置验证码已发送: userName={}", user.getUserName());
    }
}
