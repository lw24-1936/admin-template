package com.admintem.web.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一业务返回码。
 *
 * <p>约定：
 * <ul>
 *   <li>code = 200：业务成功；</li>
 *   <li>code = 400/401/403/404/500：业务失败（HTTP 状态码通常保持 200，
 *       由前端根据 code 提示；安全相关的 401/403 同时返回真实 HTTP 状态码）；</li>
 * </ul>
 * </p>
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),

    /** 请求参数错误 */
    PARAM_ERROR(400, "请求参数错误"),

    /** 未登录或令牌无效/过期 */
    UNAUTHORIZED(401, "未登录或登录已过期，请重新登录"),

    /** 无权限访问（按钮级/菜单级权限校验失败） */
    FORBIDDEN(403, "没有操作权限，请联系管理员授权"),

    /** 资源不存在 */
    NOT_FOUND(404, "请求的资源不存在"),

    /** 请求方法不支持（如用 GET 调用 POST 接口），与 HTTP 405 对应 */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /** 服务器内部错误 */
    INTERNAL_ERROR(500, "服务器内部错误"),

    /* -------------------- 业务码（5xx 段，HTTP 仍为 200） -------------------- */

    /** 用户名或密码错误 */
    USERNAME_OR_PASSWORD_ERROR(501, "用户名或密码错误"),

    /** 账号已被锁定（连续登录失败） */
    ACCOUNT_LOCKED(502, "账号已锁定，请稍后再试"),

    /** 账号已被禁用 */
    ACCOUNT_DISABLED(503, "账号已被禁用，请联系管理员"),

    /** 用户不存在 */
    USER_NOT_EXIST(504, "用户不存在"),

    /** 用户名已存在 */
    USERNAME_EXISTS(505, "用户名已存在"),

    /** 注册功能未开放 */
    REGISTER_CLOSED(506, "系统暂未开放自助注册"),

    /** 旧密码错误 */
    OLD_PASSWORD_ERROR(507, "原密码错误"),

    /** 数据被引用，禁止删除（如角色下仍有用户等） */
    DATA_IN_USE(508, "该数据仍被引用，禁止删除"),

    /** 不允许对内置数据操作（如内置管理员角色） */
    BUILT_IN_DATA(509, "内置数据不允许该操作"),

    /** 数据不存在 */
    DATA_NOT_EXIST(510, "数据不存在"),

    /**
     * 来源 IP 被临时限制登录（IP 维度风控，见 02 设计文档 3.4）。
     *
     * <p>与 {@link #ACCOUNT_LOCKED} 区分开：账号锁定是"这个账号被锁"，
     * 本码是"这个来源网络被限"，前端提示与运维告警都应区别对待
     * （前者引导用户等待，后者往往是攻击特征）。</p>
     */
    LOGIN_IP_LIMITED(511, "当前网络登录尝试过于频繁，请稍后再试");

    /** 业务状态码 */
    private final int code;

    /** 默认提示信息 */
    private final String message;
}
