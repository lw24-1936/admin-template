package com.admintem.web.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 当前登录用户（既是 Spring Security 的 UserDetails，也是 Redis 中缓存的会话快照）。
 *
 * <p>快照字段：用户基本信息 + 角色 role_key 集合 + 权限 permission_key 集合。
 * 权限集合用于 {@link RbacService} 的按钮级权限判定，避免每个请求回查数据库。</p>
 *
 * <p>注意：对象整体会序列化为 JSON 存放于 Redis，因此 password 必须忽略。</p>
 */
@Data
@NoArgsConstructor
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    /** 用户数据库主键 */
    private Long userId;

    /** 登录账号 */
    private String userName;

    /** 昵称 */
    private String nickName;

    /** 账号状态（0启用 1禁用），禁用账号不写入快照 */
    private Integer status;

    /** 密码散列（仅认证阶段使用，不序列化进 Redis） */
    @JsonIgnore
    private String password;

    /** 角色 role_key 集合 */
    private Set<String> roleKeys = new HashSet<>();

    /** 权限 permission_key 集合（菜单 + 按钮） */
    private Set<String> permissionKeys = new HashSet<>();

    /**
     * 构造基础信息。
     *
     * @param userId   用户ID
     * @param userName 账号
     * @param nickName 昵称
     * @param password 密码散列（可为空）
     * @param status   状态
     */
    public LoginUser(Long userId, String userName, String nickName, String password, Integer status) {
        this.userId = userId;
        this.userName = userName;
        this.nickName = nickName;
        this.password = password;
        this.status = status;
    }

    /* ------------------- UserDetails 接口实现（本工程不使用 GrantedAuthority） ------------------- */

    /**
     * 返回权限集合。本工程按钮级校验统一走 {@link #getPermissionKeys()} 与
     * {@link RbacService}，此处返回空集合以避免重复/冲突的自动授权规则。
     */
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.Collections.emptyList();
    }

    /**
     * Spring Security 用户名（即登录账号）。
     *
     * <p>仅服务 Spring Security 认证接口；序列化时忽略，避免与
     * {@link #getUserName()} 在 JSON 中产生重复字段（缓存快照反序列化时报
     * “未知属性 username”导致缓存永不命中，见 Redis 序列化修复说明）。</p>
     */
    @Override
    @JsonIgnore
    public String getUsername() {
        return this.userName;
    }

    /**
     * 获取登录账号。
     *
     * <p>说明：因 {@link UserDetails#getUsername()} 与 Lombok 生成的
     * {@code getUserName()} 仅大小写不同，Lombok 会抑制自动生成，
     * 这里手动补齐，保证业务代码与 JSON 序列化字段（userName）一致。</p>
     *
     * @return 登录账号
     */
    public String getUserName() {
        return this.userName;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return this.password;
    }

    /** 账号是否未过期（本工程不启用该机制，恒为 true） */
    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    /** 账号是否未锁定（锁定由 Redis 登录失败策略控制，此处恒为 true） */
    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    /** 凭据是否未过期 */
    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 是否启用（status=0 表示启用） */
    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return status != null && status == 0;
    }

    /* ------------------- 便捷判定方法 ------------------- */

    /**
     * 是否拥有指定角色。
     *
     * @param roleKey 角色唯一标识
     * @return true 表示拥有
     */
    public boolean hasRoleKey(String roleKey) {
        return roleKeys != null && roleKeys.contains(roleKey);
    }

    /**
     * 是否拥有指定权限标识。
     *
     * @param permissionKey 权限唯一标识
     * @return true 表示拥有
     */
    public boolean hasPermissionKey(String permissionKey) {
        return permissionKeys != null && permissionKeys.contains(permissionKey);
    }
}
