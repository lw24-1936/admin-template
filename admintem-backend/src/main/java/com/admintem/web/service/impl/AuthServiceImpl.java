package com.admintem.web.service.impl;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.entity.SysAuthPermission;
import com.admintem.web.entity.SysAuthRole;
import com.admintem.web.entity.SysAuthUser;
import com.admintem.web.entity.SysAuthUserRole;
import com.admintem.web.entity.dto.LoginRequest;
import com.admintem.web.entity.dto.RegisterRequest;
import com.admintem.web.entity.dto.UpdatePasswordRequest;
import com.admintem.web.entity.dto.UpdateProfileRequest;
import com.admintem.web.entity.vo.LoginVO;
import com.admintem.web.entity.vo.MenuVO;
import com.admintem.web.entity.vo.RoleVO;
import com.admintem.web.entity.vo.UserSessionVO;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysAuthPermissionMapper;
import com.admintem.web.mapper.SysAuthRoleMapper;
import com.admintem.web.mapper.SysAuthUserMapper;
import com.admintem.web.mapper.SysAuthUserRoleMapper;
import com.admintem.web.security.JwtTokenService;
import com.admintem.web.security.LoginRiskGuard;
import com.admintem.web.security.PasswordPolicy;
import com.admintem.web.security.LoginUser;
import com.admintem.web.security.RbacCacheService;
import com.admintem.web.security.RefreshTokenService;
import com.admintem.web.security.SecurityMetrics;
import com.admintem.web.security.SecurityUtils;
import com.admintem.web.service.AuthService;
import com.admintem.web.service.FileStorageService;
import com.admintem.web.service.UserRbacService;
import com.admintem.web.util.tree.TreeUtils;
import com.admintem.web.util.web.ClientIpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证服务实现：登录/注册/登出/用户会话/个人资料与密码。
 *
 * <p>数据访问均走 Mapper XML（不依赖 MyBatis-Plus 内置方法）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final RbacCacheService rbacCacheService;
    private final UserRbacService userRbacService;
    private final PasswordEncoder passwordEncoder;
    private final AdmintemProperties properties;
    /** Redisson 客户端（登录失败计数 / 账号锁定） */
    private final LoginRiskGuard loginRiskGuard;
    /** 密码策略：编码强度、复杂度校验、登录时静默升级旧散列 */
    private final PasswordPolicy passwordPolicy;
    /** 文件存储（头像）；本地目录实现，换对象存储只替换实现类 */
    private final FileStorageService fileStorageService;

    /** 可选注入：单测/轻量场景未启动指标时不阻断 */
    @Autowired(required = false)
    private SecurityMetrics securityMetrics;

    private final SysAuthUserMapper userMapper;
    private final SysAuthRoleMapper roleMapper;
    private final SysAuthPermissionMapper permissionMapper;
    private final SysAuthUserRoleMapper userRoleMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public LoginVO login(LoginRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();
        boolean remember = Boolean.TRUE.equals(request.getRememberMe());
        // 来源 IP：账号维度与 IP 维度风控共用同一口径（见 ClientIpUtils）
        String clientIp = ClientIpUtils.currentClientIp();

        // 1. 登录风控预检查（防暴力破解：账号维度 + IP 维度）
        loginRiskGuard.checkAccountLocked(username);
        loginRiskGuard.checkIpLimited(clientIp, username);

        // 2. Spring Security 认证
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            log.warn("用户登录失败: userName={}, 原因=账号已被禁用", username);
              recordLogin("failure");
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        } catch (LockedException e) {
            log.warn("用户登录失败: userName={}, 原因=账号已锁定", username);
              recordLogin("failure");
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED);
        } catch (AuthenticationException e) {
            // 用户名不存在 / 密码错误统一提示，避免泄露账号是否存在
            // 日志只记录账号、来源 IP 与失败原因，绝不记录密码明文
            log.warn("用户登录失败: userName={}, ip={}, 原因=用户名或密码错误", username, clientIp);
              recordLogin("failure");
            loginRiskGuard.onLoginFailure(username, clientIp);
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 3. 认证成功：清理失败计数
          recordLogin("success");
        loginRiskGuard.onLoginSuccess(username, clientIp);

        // 3.1 密码散列按需升级（此时明文在手，是唯一能透明升级的时机）
        //     BCrypt 的代价因子写死在散列里，提高配置值只影响新密码；老用户下次登录即被升级。
        upgradePasswordHashIfNeeded(username, password);

        // 4. 组装会话快照并缓存（快照含角色与权限集合，供后续请求直接使用）
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        rbacCacheService.save(loginUser);

        // 5. 签发令牌（写入 Redis 白名单）
        String token = jwtTokenService.createToken(loginUser.getUserId(), loginUser.getUserName(), remember);

        // 6. 组装登录响应
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setRefreshToken(refreshTokenService.issue(loginUser.getUserName(), remember));
        // 复用刚认证得到的快照，避免会话构建阶段重复查询角色与权限
        vo.setUser(buildUserSession(loginUser.getUserId(), loginUser));
        log.info("用户登录成功: userName={}, userId={}", loginUser.getUserName(), loginUser.getUserId());
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        // 1. 是否开放注册
        if (!properties.getAuth().isRegisterEnabled()) {
            throw new BusinessException(ResultCode.REGISTER_CLOSED);
        }
        // 2. 账号唯一性
        if (userMapper.countByUserName(request.getUserName(), null) > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        // 2.1 密码复杂度（配置开启时生效）
        passwordPolicy.validateComplexity(request.getPassword());
        // 3. 创建用户（默认启用，密码 BCrypt 加密）
        SysAuthUser user = new SysAuthUser();
        user.setUserName(request.getUserName());
        user.setNickName(request.getNickName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setSex(request.getSex());
        user.setStatus(0);
        user.setCreatedBy(request.getUserName());
        user.setCreatedTime(LocalDateTime.now());
        userMapper.insert(user);
        log.info("用户自助注册成功: id={}, userName={}", user.getId(), user.getUserName());

        // 4. 分配默认角色（普通用户角色，由数据库脚本预置；缺失时仅告警）
        assignDefaultRole(user.getId(), user.getUserName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoginVO refresh(String refreshToken) {
        String userName = refreshTokenService.consume(refreshToken);
        if (userName == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        SysAuthUser user = userMapper.selectByUserName(userName);
        if (user == null || !Integer.valueOf(0).equals(user.getStatus())) {
            refreshTokenService.revokeAll(userName);
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        LoginUser loginUser = userRbacService.loadLoginUserSnapshot(user.getId(), userName);
        if (loginUser == null || !loginUser.isEnabled()) {
            refreshTokenService.revokeAll(userName);
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String token = jwtTokenService.createToken(user.getId(), userName, false);
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setRefreshToken(refreshTokenService.issue(userName, false));
        vo.setUser(buildUserSession(user.getId(), loginUser));
        log.info("刷新访问令牌成功: userName={}, userId={}", userName, user.getId());
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logout(Long userId, String userName) {
        // 删除令牌白名单，使服务端令牌立即失效
        jwtTokenService.revokeToken(userName);
        refreshTokenService.revokeAll(userName);
        rbacCacheService.evictUser(userId);
        log.info("用户退出登录: userId={}, userName={}", userId, userName);
    }

    /**
     * {@inheritDoc}
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public UserSessionVO buildUserSession(Long userId) {
        // 已认证请求（如 /auth/getUserInfo）可从安全上下文取到登录快照，复用其中的角色与权限标识
        return buildUserSession(userId, SecurityUtils.currentUser());
    }

    /**
     * 构建用户会话（内部实现）：可用已认证快照减少查询。
     *
     * <p>查询优化（本轮加固）：快照里已带 roleKeys 与 permissionKeys，
     * 因此不再"按用户查角色关联 → 按 ID 查角色 → 再查一次关联 → 查权限表"，
     * 而是按 roleKeys / permissionKeys 各查一次；超级管理员的菜单仍取全量权限行。
     * 实测：/auth/getUserInfo 由 4~5 条降为 3 条，登录由 10 条降为 8 条。</p>
     *
     * @param userId    用户ID
     * @param principal 已认证的登录快照（可为 null，此时退回按用户ID查询）
     * @return 会话视图
     */
    private UserSessionVO buildUserSession(Long userId, LoginUser principal) {
        // 1. 用户基础信息
        SysAuthUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        // 2. 角色：优先用快照里的 roleKeys（1 条 SQL），否则按用户ID查询（2 条 SQL）
        boolean usePrincipalRoleKeys = principal != null && !CollectionUtils.isEmpty(principal.getRoleKeys());
        List<SysAuthRole> roles = usePrincipalRoleKeys
                ? userRbacService.listRolesByRoleKeys(principal.getRoleKeys())
                : userRbacService.listRolesByUserId(userId);
        boolean admin = roles.stream().anyMatch(r -> isAdminRoleKey(r.getRoleKey()));

        /*
         * 3. 权限行与权限标识（一份数据两用）：
         *    - 超级管理员：全量表一次（含全部菜单），标识取其中启用项；
         *    - 普通用户且有快照：按快照的 permissionKeys 取行（不再全表扫描、不重复查关联）；
         *    - 其余情况：按用户的角色授权链路取行。
         */
        List<SysAuthPermission> permissionRows;
        Set<String> permissionKeys;
        if (admin) {
            permissionRows = permissionMapper.selectAllOrdered();
            permissionKeys = listAllEnabledKeys(permissionRows);
        } else if (principal != null && !CollectionUtils.isEmpty(principal.getPermissionKeys())) {
            permissionRows = permissionMapper.selectByKeys(principal.getPermissionKeys());
            permissionKeys = userRbacService.toEnabledPermissionKeys(permissionRows);
        } else {
            permissionRows = userRbacService.listPermissionsByUserId(userId);
            permissionKeys = userRbacService.toEnabledPermissionKeys(permissionRows);
        }

        // 4. 可访问菜单树
        List<MenuVO> menus = buildMenus(admin, permissionKeys, permissionRows);

        // 5. 组装
        UserSessionVO session = new UserSessionVO();
        session.setUserId(user.getId());
        session.setUserName(user.getUserName());
        session.setNickName(user.getNickName());
        session.setAvatar(user.getAvatar());
        session.setEmail(user.getEmail());
        session.setPhone(user.getPhone());
        session.setSex(user.getSex());
        session.setIntroduce(user.getIntroduce());
        session.setStatus(user.getStatus());
        session.setRoles(roles.stream().map(this::toRoleVO).collect(Collectors.toList()));
        session.setMenus(menus);
        session.setPermissions(permissionKeys);
        return session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        SysAuthUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (request.getNickName() != null) {
            user.setNickName(request.getNickName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getSex() != null) {
            user.setSex(request.getSex());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getIntroduce() != null) {
            user.setIntroduce(request.getIntroduce());
        }
        user.setUpdateBy(user.getUserName());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateSelective(user);
        // 资料变化清理缓存（下次请求自动回源刷新）
        rbacCacheService.evictUser(userId);
        log.info("用户修改个人资料: userId={}", userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        SysAuthUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        // 校验原密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        // 复杂度校验（配置开启时生效；默认关闭以免影响既有演示账号）
        passwordPolicy.validateComplexity(request.getNewPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateBy(user.getUserName());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateSelective(user);
        // 修改密码后强制该账号重新登录（白名单删除 + 缓存清理）
        jwtTokenService.revokeToken(user.getUserName());
        refreshTokenService.revokeAll(user.getUserName());
        rbacCacheService.evictUser(userId);
        log.info("用户修改密码成功: userId={}", userId);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        SysAuthUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        // 1. 落盘（校验都在存储层：大小/扩展名/文件头）
        String oldAvatar = user.getAvatar();
        String url = fileStorageService.storeAvatar(file);
        // 2. 更新用户头像字段：只更新这一列，避免连带覆盖其他资料
        SysAuthUser update = new SysAuthUser();
        update.setId(userId);
        update.setAvatar(url);
        update.setUpdateBy(user.getUserName());
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateSelective(update);
        // 3. 清理旧头像（best effort：失败不回滚，也不影响本次上传）
        if (oldAvatar != null && !oldAvatar.isBlank() && !oldAvatar.equals(url)) {
            try {
                fileStorageService.deleteByUrl(oldAvatar);
            } catch (Exception e) {
                log.warn("旧头像清理失败: userId={}, url={}, err={}", userId, oldAvatar, e.getMessage());
            }
        }
        // 4. 会话快照里也带 avatar，清掉缓存让下次读取拿到新值
        rbacCacheService.evictUser(userId);
        log.info("用户头像已更新: userId={}, url={}", userId, url);
        return url;
    }

    /* ------------------------------ 内部方法 ------------------------------ */

    /**
     * 登录成功后按需把密码散列升级到当前代价因子。
     *
     * <p>只在"确实需要升级"时才多查一次库、多写一次库，正常登录路径零额外开销。
     * 失败只记 WARN：升级是增强动作，不能反过来让用户登录失败。</p>
     *
     * @param username 登录账号
     * @param rawPassword 本次提交的明文（此刻已验证正确）
     */
    private void recordLogin(String result) {
        if (securityMetrics != null) {
            securityMetrics.login(result);
        }
    }

    private void upgradePasswordHashIfNeeded(String username, String rawPassword) {
        try {
            SysAuthUser current = userMapper.selectByUserName(username);
            if (current == null || !passwordPolicy.needsRehash(current.getPassword())) {
                return;
            }
            SysAuthUser update = new SysAuthUser();
            update.setId(current.getId());
            update.setPassword(passwordPolicy.encode(rawPassword));
            update.setUpdateBy("system:rehash");
            update.setUpdateTime(LocalDateTime.now());
            userMapper.updateSelective(update);
            log.info("密码散列已升级到当前代价因子: userName={}", username);
        } catch (Exception e) {
            log.warn("密码散列升级失败（不影响本次登录）: userName={}, err={}", username, e.getMessage());
        }
    }

    /** 是否为超级角色 key */
    private boolean isAdminRoleKey(String roleKey) {
        return properties.getSecurity().getAdminRoleKey().equals(roleKey);
    }

    /** 注册后分配默认角色 */
    private void assignDefaultRole(Long userId, String operator) {
        String defaultKey = properties.getAuth().getDefaultRoleKey();
        SysAuthRole defaultRole = roleMapper.selectByRoleKey(defaultKey);
        if (defaultRole == null) {
            log.warn("未找到默认角色(role_key={})，注册用户暂未分配角色，请执行数据库初始化脚本", defaultKey);
            return;
        }
        SysAuthUserRole link = new SysAuthUserRole();
        link.setUserId(userId);
        link.setRoleId(defaultRole.getId());
        link.setCreatedBy(operator);
        link.setCreatedTime(LocalDateTime.now());
        userRoleMapper.insert(link);
    }

    /**
     * 构建用户可访问菜单树。
     *
     * @param admin          是否超级管理员（放行全部菜单）
     * @param permissionKeys 用户权限标识集合
     * @param menuRows       全量权限行（调用方已查询，避免重复查库）
     * @return 菜单树（孤儿节点提升为根，避免授权不全导致菜单丢失）
     */
    private List<MenuVO> buildMenus(boolean admin, Set<String> permissionKeys, List<SysAuthPermission> menuRows) {
        List<MenuVO> nodes = new ArrayList<>();
        for (SysAuthPermission p : menuRows) {
            // 仅保留菜单类型（type=0）且启用（status 空视为启用，兼容历史数据）
            if (p.getType() != null && p.getType() != 0) {
                continue;
            }
            if (p.getStatus() != null && p.getStatus() == 1) {
                continue;
            }
            if (!admin) {
                // 非管理员：仅返回拥有对应菜单权限标识的节点
                if (!StringUtils.hasText(p.getPermissionKey())
                        || !permissionKeys.contains(p.getPermissionKey())) {
                    continue;
                }
            }
            MenuVO node = new MenuVO();
            node.setId(p.getId());
            node.setName(p.getName());
            // 菜单树中均视为菜单类型（type=0），供前端动态路由识别叶子
            node.setType(0);
            node.setParentId(p.getParentId());
            node.setOrderNum(p.getOrderNum());
            node.setMenuUrl(p.getMenuUrl());
            node.setComponent(p.getComponent());
            node.setIcon(p.getIcon());
            node.setShow(p.getShow());
            node.setPermissionKey(p.getPermissionKey());
            nodes.add(node);
        }
        return TreeUtils.buildTree(nodes, true);
    }

    /**
     * 全部启用的权限标识（超级管理员用）。
     *
     * @param permissionRows 全量权限行（调用方已查询）
     */
    private Set<String> listAllEnabledKeys(List<SysAuthPermission> permissionRows) {
        Set<String> keys = new LinkedHashSet<>();
        for (SysAuthPermission p : permissionRows) {
            boolean enabled = p.getStatus() == null || p.getStatus() == 0;
            if (enabled && StringUtils.hasText(p.getPermissionKey())) {
                keys.add(p.getPermissionKey());
            }
        }
        return keys;
    }

    /** 角色实体 → VO */
    private RoleVO toRoleVO(SysAuthRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleKey(role.getRoleKey());
        return vo;
    }

    /* ------------------- 登录失败锁定（防暴力破解） ------------------- */
    /* 具体策略已收敛到 security/LoginRiskGuard：账号维度（默认开启）+ IP 维度（默认关闭，
       见 02 设计文档 3.4）。真实实现见该类，本类只负责调用。 */
}
