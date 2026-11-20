package com.admintem.web.entity.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录结果视图：令牌 + 用户会话。
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 访问令牌（前端请求头：admintem_token: LAUNCH &lt;token&gt;） */
    private String token;

    /** 刷新令牌（仅用于 /auth/refresh；不放进请求头） */
    private String refreshToken;

    /** 用户会话（角色/菜单/权限） */
    private UserSessionVO user;
}
