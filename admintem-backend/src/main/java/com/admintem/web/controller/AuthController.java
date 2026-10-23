package com.admintem.web.controller;

import com.admintem.web.aspect.AuditLog;
import com.admintem.web.aspect.AuditTargetType;
import com.admintem.web.common.Result;
import com.admintem.web.entity.dto.CaptchaVerifyRequest;
import com.admintem.web.entity.dto.LoginRequest;
import com.admintem.web.entity.vo.CaptchaTicketVO;
import com.admintem.web.entity.vo.CaptchaVO;
import com.admintem.web.security.CaptchaService;
import com.admintem.web.entity.dto.RegisterRequest;
import com.admintem.web.entity.dto.UpdatePasswordRequest;
import com.admintem.web.entity.dto.UpdateProfileRequest;
import com.admintem.web.security.LoginUser;
import com.admintem.web.security.SecurityUtils;
import com.admintem.web.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 认证控制器：登录 / 注册 / 登出 / 当前用户信息 / 个人资料与密码。
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "01-认证接口", description = "登录、注册、登出、用户信息、个人资料与密码")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    /**
     * 用户登录（免登录）。
     *
     * @param request 登录请求体
     * @return 令牌 + 用户会话
     */
    /** 生成图形验证码（免登录）。 */
    @PostMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(captchaService.create());
    }

    /** 校验拼图位置，通过返回一次性票据（免登录）。 */
    @PostMapping("/captcha/verify")
    public Result<CaptchaTicketVO> verifyCaptcha(@Valid @RequestBody CaptchaVerifyRequest request) {
        return Result.ok(new CaptchaTicketVO(captchaService.verify(request.getToken(), request.getCode())));
    }

    @PostMapping("/login")
    @AuditLog(action = "LOGIN", name = "用户登录", target = AuditTargetType.AUTH)
    @Operation(summary = "登录")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        captchaService.assertTicket(request.getCaptchaTicket());
        return Result.ok("登录成功", authService.login(request));
    }

    /**
     * 用户自助注册（免登录；是否开放由配置 register-enabled 决定）。
     *
     * @param request 注册请求体
     * @return 成功提示
     */
    @PostMapping("/register")
    @AuditLog(action = "REGISTER", name = "自助注册", target = AuditTargetType.AUTH)
    @Operation(summary = "注册")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.ok("注册成功，请登录", null);
    }

    /**
     * 退出登录：删除令牌白名单，服务端令牌立即失效。
     *
     * @return 成功提示
     */
    @PostMapping("/logout")
    @AuditLog(action = "LOGOUT", name = "退出登录", target = AuditTargetType.AUTH)
    @Operation(summary = "退出登录")
    public Result<Void> logout() {
        LoginUser user = SecurityUtils.currentUser();
        authService.logout(user.getUserId(), user.getUserName());
        return Result.ok("退出成功", null);
    }

    /**
     * 获取当前用户完整会话（角色/菜单/权限），用于刷新前端权限。
     *
     * @return 用户会话
     */
    @PostMapping("/getUserInfo")
    @Operation(summary = "获取当前用户信息（含菜单与权限）")
    public Result<?> getUserInfo() {
        return Result.ok(authService.buildUserSession(SecurityUtils.currentUserId()));
    }

    /**
     * 修改本人资料。
     *
     * @param request 资料请求体
     * @return 成功提示
     */
    @PostMapping("/updateProfile")
    @AuditLog(action = "SELF_UPDATE_PROFILE", name = "修改个人资料", target = AuditTargetType.SELF)
    @Operation(summary = "修改个人资料")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        authService.updateProfile(SecurityUtils.currentUserId(), request);
        return Result.ok();
    }

    /**
     * 修改本人密码（成功后需重新登录）。
     *
     * @param request 密码请求体
     * @return 成功提示
     */
    @PostMapping("/updatePassword")
    @AuditLog(action = "SELF_UPDATE_PASSWORD", name = "修改本人密码", target = AuditTargetType.SELF)
    @Operation(summary = "修改本人密码")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        authService.updatePassword(SecurityUtils.currentUserId(), request);
        return Result.ok("密码修改成功，请重新登录", null);
    }

    /**
     * 上传并更新当前用户头像（自服务，登录即可，无需额外权限标识）。
     *
     * <p>使用 multipart/form-data，字段名固定为 {@code file}。
     * 校验全部在服务端（大小 / 扩展名 / 文件头），前端的大小提示只是体验优化。</p>
     *
     * @param file 图片文件（png/jpg/jpeg/webp/gif，≤ 配置上限）
     * @return 可访问的相对路径（前端按 API 基址拼完整 URL），例如 `/uploads/avatar/202610/xxx.png`
     */
    @PostMapping(value = "/uploadAvatar", consumes = "multipart/form-data")
    @Operation(summary = "上传头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        LoginUser current = SecurityUtils.currentUser();
        String url = authService.uploadAvatar(current.getUserId(), file);
        return Result.ok(url);
    }}
