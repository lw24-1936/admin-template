package com.admintem.web.service;

/** 自助密码找回（验证码 + 重置）。 */
public interface PasswordResetService {

    /** 请求验证码；无论账号是否存在都返回相同语义，避免账号枚举。 */
    void requestCode(String userName);

    /** 校验验证码并重置密码，成功后吊销该账号全部会话。 */
    void reset(String userName, String code, String newPassword);
}
