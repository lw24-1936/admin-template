package com.admintem.web.util.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 客户端 IP 解析（单一出处）。
 *
 * <p>为什么抽成工具类：审计留痕与登录风控都需要"请求来自哪个 IP"，
 * 两处各写一遍就会出现"审计记录的是 X-Forwarded-For、风控统计的是 remoteAddr"
 * 这类口径漂移——统计与追溯对不上，排查时会非常被动。</p>
 *
 * <p>取值优先级：{@code X-Forwarded-For} 首跳 → {@code X-Real-IP} → {@code remoteAddr}。
 * 只有经过反向代理（Nginx 等）时前两者才有意义；直连时回落真实来源地址。</p>
 */
public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    /**
     * 解析请求来源 IP。
     *
     * @param request 当前请求（可为 null）
     * @return IP 字符串；无法解析时返回 null
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 从当前线程绑定的请求上下文解析来源 IP。
     *
     * <p>适用于 Service 层：不必在方法签名上暴露 Servlet 类型，
     * 非 Web 线程（定时任务等）返回 null，由调用方决定如何退化。</p>
     *
     * @return IP 字符串；无请求上下文时为 null
     */
    public static String currentClientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return getClientIp(attributes.getRequest());
        }
        return null;
    }
}
