package com.admintem.web.controller;

import com.admintem.web.common.Result;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.entity.dto.KickSessionRequest;
import com.admintem.web.entity.vo.OnlineSessionVO;
import com.admintem.web.service.OnlineSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 在线会话控制器（查看 + 强制下线）。
 *
 * <p>权限：查看需要菜单权限 {@code onlineSession:onlineSession}；
 * 强制下线需要按钮权限 {@code sysAuthSession:kick}（与前端 {@code v-perm} 同一标识）。</p>
 *
 * <p>"在线"的依据是 Redis 令牌白名单，不额外维护在线表（见 {@link OnlineSessionService}）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/sysAuthSession")
@RequiredArgsConstructor
@Tag(name = "07-在线会话", description = "查看在线账号与强制下线")
public class SysAuthSessionController {

    private final OnlineSessionService onlineSessionService;
    private final AdmintemProperties properties;

    /**
     * 查询在线会话列表。
     *
     * <p>当前请求的令牌会作为参数传下去，用于在列表里标记"这是你自己这条"——
     * 避免把当前会话误踢（前端会禁用当前行的下线按钮）。</p>
     *
     * @param request 当前请求（读取令牌请求头）
     * @return 在线会话列表
     */
    @PostMapping("/queryOnline")
    @PreAuthorize("@rbac.hasPerm('onlineSession:onlineSession')")
    @Operation(summary = "查询在线会话")
    public Result<List<OnlineSessionVO>> queryOnline(HttpServletRequest request) {
        return Result.ok(onlineSessionService.listSessions(currentToken(request)));
    }

    /**
     * 强制账号下线。
     *
     * @param body 目标账号
     * @return 成功提示（原本就不在线也返回成功，语义是"操作后该账号已无有效令牌"）
     */
    @PostMapping("/kick")
    @PreAuthorize("@rbac.hasPerm('sysAuthSession:kick')")
    @Operation(summary = "强制下线")
    public Result<Void> kick(@Valid @RequestBody KickSessionRequest body) {
        boolean existed = onlineSessionService.kick(body.getUserName());
        log.info("强制下线完成: userName={}, 原会话存在={}", body.getUserName(), existed);
        return Result.ok();
    }

    /**
     * 从请求头取出裸令牌。
     *
     * <p>请求头形如 {@code admintem_token: "LAUNCH <jwt>"}，前缀从配置读取，
     * 前缀不匹配时返回 null（等价于"无法识别当前会话"，不影响列表展示）。</p>
     */
    private String currentToken(HttpServletRequest request) {
        // 请求头名与前缀都取自配置（与 JwtAuthenticationFilter 同一来源），
        // 避免把 "admintem_token" / "LAUNCH " 这类约定写死在两处
        String headerValue = request.getHeader(properties.getJwt().getHeader());
        if (headerValue == null) {
            return null;
        }
        String prefix = properties.getJwt().getBearerPrefix();
        return headerValue.startsWith(prefix) ? headerValue.substring(prefix.length()).trim() : headerValue.trim();
    }
}
