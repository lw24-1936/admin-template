package com.admintem.web.service;

import com.admintem.web.entity.SysAuthPermission;
import com.admintem.web.entity.SysAuthRole;
import com.admintem.web.security.LoginUser;

import java.util.Collection;
import java.util.List;

/**
 * 用户 RBAC 数据加载服务。
 *
 * <p>负责根据用户 ID 从数据库加载其角色与权限集合，组装成 {@link LoginUser} 会话快照；
 * 供登录、令牌过滤器缓存未命中回源、以及权限缓存失效后的重载使用。</p>
 */
public interface UserRbacService {

    /**
     * 加载用户的完整会话快照（含角色 role_key 与权限 permission_key）。
     *
     * @param userId   用户ID
     * @param userName 登录账号（用于兜底回填，可为 null）
     * @return LoginUser（账号不存在或已删除返回 null）
     */
    LoginUser loadLoginUserSnapshot(Long userId, String userName);

    /**
     * 查询用户拥有的全部角色。
     *
     * @param userId 用户ID
     * @return 角色列表（自动过滤逻辑删除）
     */
    List<SysAuthRole> listRolesByUserId(Long userId);

    /**
     * 查询用户拥有的角色 ID 集合（不含角色明细）。
     *
     * <p>会话构建需要"角色明细 + 权限"两份数据，二者都依赖同一份角色 ID；
     * 先取一次 ID 再分别加载，可避免同一份关联被重复查询（见 buildUserSession）。</p>
     *
     * @param userId 用户ID
     * @return 角色 ID 列表（无角色返回空列表）
     */
    List<Long> listRoleIdsByUserId(Long userId);

    /**
     * 按角色 ID 集合批量查询角色明细。
     *
     * @param roleIds 角色 ID 集合
     * @return 角色列表（自动过滤逻辑删除）
     */
    List<SysAuthRole> listRolesByIds(Collection<Long> roleIds);

    /**
     * 按角色标识（role_key）集合批量查询角色明细。
     *
     * <p>会话构建已持有登录快照中的 roleKeys，用它直接取角色明细，
     * 可省去"按用户查关联 + 按 ID 查角色"两次查询（登录与 /auth/getUserInfo 热路径）。</p>
     *
     * @param roleKeys 角色标识集合
     * @return 角色列表（自动过滤逻辑删除）
     */
    List<SysAuthRole> listRolesByRoleKeys(Collection<String> roleKeys);

    /**
     * 按用户查询其被授予的权限行（内部先取角色 ID 再取权限，等价于按角色授权链路）。
     *
     * @param userId 用户ID
     * @return 权限行列表（无授权返回空列表）
     */
    List<SysAuthPermission> listPermissionsByUserId(Long userId);

    /**
     * 按角色 ID 集合查询其被授予的权限行（菜单 + 按钮，含禁用项）。
     *
     * <p>会话构建复用同一份结果：既据此生成权限标识集合（仅启用项），
     * 也据此装配菜单树（{@code buildMenus} 内部再按 status 过滤），
     * 因此无需再全表扫描一次 sys_auth_permission。</p>
     *
     * @param roleIds 角色 ID 集合
     * @return 权限行列表（无授权返回空列表）
     */
    List<SysAuthPermission> listPermissionsByRoleIds(Collection<Long> roleIds);

    /**
     * 查询用户拥有的权限标识集合（菜单 + 按钮，仅启用项）。
     *
     * @param userId 用户ID
     * @return permission_key 集合
     */
    java.util.Set<String> listPermissionKeysByUserId(Long userId);

    /**
     * 权限行 → 启用的 permission_key 集合（status 空视为启用，兼容历史数据）。
     *
     * <p>登录快照与会话构建共用同一口径，避免两处各自实现导致过滤规则漂移。</p>
     *
     * @param permissions 权限行（可为空）
     * @return permission_key 集合
     */
    java.util.Set<String> toEnabledPermissionKeys(List<SysAuthPermission> permissions);

    /**
     * 根据角色 ID 集合反查拥有这些角色的用户 ID（用于权限变更后的缓存清理）。
     *
     * @param roleIds 角色 ID 集合
     * @return 用户 ID 列表
     */
    List<Long> listUserIdsByRoleIds(Collection<Long> roleIds);
}
