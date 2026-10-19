package com.admintem.web.service;

import com.admintem.web.entity.vo.OnlineSessionVO;

import java.util.List;

/**
 * 在线会话服务（基于 Redis 中的令牌白名单）。
 *
 * <p>本服务不新增任何存储：</p>
 * <ul>
 *   <li>"谁在线" = Redis 里存在 {@code admintem:auth:jwt:<账号>} 且未过期；</li>
 *   <li>"强制下线" = 删除该 key（与用户主动退出登录走同一条路径），
 *       之后该账号的令牌在过滤器里校验失败，下一次请求即被拒绝。</li>
 * </ul>
 */
public interface OnlineSessionService {

    /**
     * 列出当前在线会话。
     *
     * @param currentToken 当前请求携带的令牌（用于标记"当前这条"；可为 null）
     * @return 会话列表（按剩余有效期倒序，最多 200 条，避免异常情况下拖垮接口）
     */
    List<OnlineSessionVO> listSessions(String currentToken);

    /**
     * 强制目标账号下线（删除其令牌白名单）。
     *
     * @param userName 目标账号
     * @return true 表示确实存在在线会话并被清除；false 表示本来就无会话
     */
    boolean kick(String userName);
}
