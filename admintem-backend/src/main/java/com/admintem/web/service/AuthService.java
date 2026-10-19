package com.admintem.web.service;

import com.admintem.web.entity.dto.LoginRequest;
import com.admintem.web.entity.dto.RegisterRequest;
import com.admintem.web.entity.dto.UpdatePasswordRequest;
import org.springframework.web.multipart.MultipartFile;
import com.admintem.web.entity.dto.UpdateProfileRequest;
import com.admintem.web.entity.vo.LoginVO;
import com.admintem.web.entity.vo.UserSessionVO;

/**
 * 认证服务：登录 / 注册 / 登出 / 用户信息 / 个人资料与密码。
 */
public interface AuthService {

    /**
     * 登录：认证成功后签发令牌并返回用户会话（角色/菜单/权限）。
     *
     * @param request 登录请求体
     * @return 登录结果
     */
    LoginVO login(LoginRequest request);

    /**
     * 自助注册：创建用户并分配默认角色（是否开放见配置 register-enabled）。
     *
     * @param request 注册请求体
     */
    void register(RegisterRequest request);

    /**
     * 登出：删除 Redis 令牌白名单（服务端令牌立即失效）。
     *
     * @param userId   当前用户ID
     * @param userName 当前用户账号
     */
    void logout(Long userId, String userName);

    /**
     * 构建用户会话视图（角色/菜单/权限），供登录与获取用户信息使用。
     *
     * @param userId 用户ID
     * @return 会话视图
     */
    UserSessionVO buildUserSession(Long userId);

    /**
     * 修改本人资料。
     *
     * @param userId  当前用户ID
     * @param request 资料请求体
     */
    void updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 修改本人密码（成功后强制当前账号下线，需重新登录）。
     *
     * @param userId  当前用户ID
     * @param request 密码请求体
     */
    void updatePassword(Long userId, UpdatePasswordRequest request);

    /**
     * 上传并更新当前用户头像。
     *
     * <p>自服务能力：不需要额外权限标识（登录即可），但只能改自己的头像。</p>
     *
     * @param userId 当前用户ID
     * @param file   上传的图片
     * @return 可访问的相对路径（前端按 API 基址拼完整 URL）
     */
    String uploadAvatar(Long userId, MultipartFile file);
}
