package com.admintem.web.entity.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录结果视图：令牌 + 用户会话。
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否需要 MFA 二次验证；为 true 时 token/refreshToken/user 为空 */
    private Boolean mfaRequired;

    /** MFA 挑战票据（仅 mfaRequired=true 时返回，5 分钟有效） */
    private String mfaToken;

    /** 访问令牌（前端请求头：admintem_token: LAUNCH &lt;token&gt;） */
    private String token;

    /** 刷新令牌（仅用于 /auth/refresh；不放进请求头） */
    private String refreshToken;

    /** 用户会话（角色/菜单/权限） */
    private UserSessionVO user;
}
