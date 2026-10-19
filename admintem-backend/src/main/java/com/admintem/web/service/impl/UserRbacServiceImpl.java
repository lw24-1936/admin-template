package com.admintem.web.service.impl;

import com.admintem.web.entity.SysAuthPermission;
import com.admintem.web.entity.SysAuthRole;
import com.admintem.web.entity.SysAuthRolePermission;
import com.admintem.web.entity.SysAuthUser;
import com.admintem.web.mapper.SysAuthPermissionMapper;
import com.admintem.web.mapper.SysAuthRoleMapper;
import com.admintem.web.mapper.SysAuthRolePermissionMapper;
import com.admintem.web.mapper.SysAuthUserMapper;
import com.admintem.web.mapper.SysAuthUserRoleMapper;
import com.admintem.web.security.LoginUser;
import com.admintem.web.service.UserRbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户 RBAC 数据加载服务实现。
 *
 * <p>查询链路（均为简单主键/等值查询，数据量小、且结果会被 Redis 缓存，因此不引入
 * 复杂联表 SQL，保证可读性与可维护性）：
 * <pre>
 *  用户 ─(sys_auth_user_role)→ 角色 ─(sys_auth_role_permission)→ 权限
 * </pre>
 * 所有 SQL 统一维护在 Mapper XML 中，逻辑删除条件由 SQL 显式保证。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRbacServiceImpl implements UserRbacService {

    private final SysAuthUserMapper userMapper;
    private final SysAuthRoleMapper roleMapper;
    private final SysAuthPermissionMapper permissionMapper;
    private final SysAuthUserRoleMapper userRoleMapper;
    private final SysAuthRolePermissionMapper rolePermissionMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public LoginUser loadLoginUserSnapshot(Long userId, String userName) {
        // 1. 定位用户（优先按 ID，否则按账号；不存在返回 null）
        SysAuthUser user;
        if (userId != null) {
            user = userMapper.selectById(userId);
        } else {
            user = userMapper.selectByUserName(userName);
        }
        if (user == null) {
            return null;
        }

        // 2. 组装基础信息（密码散列仅用于认证比对，不写入 Redis）
        LoginUser loginUser = new LoginUser(
                user.getId(), user.getUserName(), user.getNickName(), user.getPassword(), user.getStatus());

        // 3. 角色 ID 只查一次：角色明细与权限加载复用同一份关联结果
        //    （原实现分别调用 listRolesByUserId 与 listPermissionKeysByUserId，各查一次关联表）
        List<Long> roleIds = listRoleIdsByUserId(user.getId());

        // 4. 加载角色 role_key 集合
        Set<String> roleKeys = new LinkedHashSet<>();
        for (SysAuthRole role : listRolesByIds(roleIds)) {
            if (StringUtils.hasText(role.getRoleKey())) {
                roleKeys.add(role.getRoleKey());
            }
        }
        loginUser.setRoleKeys(roleKeys);

        // 5. 加载权限 permission_key 集合（同一份 roleIds 复用，避免重复查询）
        loginUser.setPermissionKeys(toEnabledPermissionKeys(listPermissionsByRoleIds(roleIds)));
        return loginUser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysAuthRole> listRolesByUserId(Long userId) {
        return listRolesByIds(userRoleMapper.selectRoleIdsByUserId(userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> listRoleIdsByUserId(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        return CollectionUtils.isEmpty(roleIds) ? Collections.emptyList() : roleIds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysAuthRole> listRolesByIds(Collection<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return roleMapper.selectByIds(roleIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysAuthRole> listRolesByRoleKeys(Collection<String> roleKeys) {
        if (CollectionUtils.isEmpty(roleKeys)) {
            return Collections.emptyList();
        }
        List<String> keys = roleKeys.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectByRoleKeys(keys);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysAuthPermission> listPermissionsByUserId(Long userId) {
        return listPermissionsByRoleIds(listRoleIdsByUserId(userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysAuthPermission> listPermissionsByRoleIds(Collection<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        // 角色-权限关联 → 权限 ID → 权限明细（XML 已过滤逻辑删除）
        List<SysAuthRolePermission> relations = rolePermissionMapper.selectByRoleIds(roleIds);
        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyList();
        }
        List<Long> permissionIds = relations.stream()
                .map(SysAuthRolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionMapper.selectByIds(permissionIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> listPermissionKeysByUserId(Long userId) {
        return toEnabledPermissionKeys(listPermissionsByRoleIds(listRoleIdsByUserId(userId)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> toEnabledPermissionKeys(List<SysAuthPermission> permissions) {
        Set<String> keys = new HashSet<>();
        if (CollectionUtils.isEmpty(permissions)) {
            return keys;
        }
        for (SysAuthPermission p : permissions) {
            boolean enabled = p.getStatus() == null || p.getStatus() == 0;
            if (enabled && StringUtils.hasText(p.getPermissionKey())) {
                keys.add(p.getPermissionKey());
            }
        }
        return keys;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> listUserIdsByRoleIds(Collection<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return userRoleMapper.selectUserIdsByRoleIds(roleIds);
    }
}
