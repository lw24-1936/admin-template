package com.admintem.web.constant;

/**
 * 安全相关常量。
 *
 * <p>令牌请求头、令牌前缀沿用旧项目的约定：
 * 前端在每个请求头携带 {@code admintem_token: LAUNCH <jwt>}，
 * 以便旧项目与新项目在同一套请求规范下平滑迁移。</p>
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    /** 令牌请求头名称 */
    public static final String TOKEN_HEADER = "admintem_token";

    /** 令牌前缀（去掉前缀后为原始 JWT） */
    public static final String TOKEN_PREFIX = "LAUNCH ";

    /** JWT 载荷中存放用户 ID 的 claim 键名 */
    public static final String CLAIM_USER_ID = "userId";

    /** JWT 载荷中存放"记住我"标记的 claim 键名 */
    public static final String CLAIM_REMEMBER = "remember";
}
