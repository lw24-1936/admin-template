package com.admintem.web.service;

import com.admintem.web.entity.SysAuthUser;

/** 密码重置验证码发送渠道；默认 log，生产可切 smtp。 */
public interface PasswordResetNotifier {

    void send(SysAuthUser user, String code);
}
