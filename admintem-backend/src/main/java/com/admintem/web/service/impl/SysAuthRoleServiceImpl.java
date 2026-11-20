package com.admintem.web.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.entity.SysAuthPermission;
import com.admintem.web.entity.SysAuthRole;
import com.admintem.web.entity.SysAuthRolePermission;
import com.admintem.web.entity.dto.AssignPermissionRequest;
import com.admintem.web.entity.dto.RolePageQuery;
import com.admintem.web.entity.dto.RoleSaveRequest;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.entity.vo.PermissionVO;
import com.admintem.web.entity.vo.RolePermVO;
import com.admintem.web.entity.vo.RoleVO;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysAuthPermissionMapper;
import com.admintem.web.mapper.SysAuthRoleMapper;
import com.admintem.web.mapper.SysAuthRolePermissionMapper;
import com.admintem.web.mapper.SysAuthUserRoleMapper;
import com.admintem.web.security.RbacCacheService;
import com.admintem.web.service.SysAuthRoleService;
import com.admintem.web.service.UserRbacService;
import com.admintem.web.support.AfterCommitExecutor;
import com.admintem.web.util.query.SortWhitelist;
import com.admintem.web.util.tree.TreeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色管理服务实现（数据访问统一走 Mapper XML）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAuthRoleServiceImpl implements SysAuthRoleService {

    private final SysAuthRoleMapper roleMapper;
    private final SysAuthUserRoleMapper userRoleMapper;
    private final SysAuthRolePermissionMapper rolePermissionMapper;
    private final SysAuthPermissionMapper permissionMapper;
    private final UserRbacService userRbacService;
    private final RbacCacheService rbacCacheService;
    private final AdmintemProperties properties;
    /** 缓存失效统一挂到事务提交之后（见 AfterCommitExecutor 的时序说明） */
    private final AfterCommitExecutor afterCommitExecutor;

    /**
     * 角色列表排序白名单：前端逻辑字段名 → 数据库列名。
     *
     * <p>未登记字段由 {@link SortWhitelist#resolve} 返回 null 并回落默认排序（id ASC）。</p>
     */
    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "id", "id",
            "roleName", "role_name",
            "roleKey", "role_key",
            "createdTime", "created_time");

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RoleVO> listAll() {
        return roleMapper.selectAllOrdered().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<RoleVO> pageQuery(RolePageQuery query) {
        // 分页参数兜底：@RequestBody 显式传 null 时 Integer 拆箱会抛 NPE（HTTP 500）
        long pageNum = query.getPageNum() == null ? 1L : query.getPageNum();
        long pageSize = query.getPageSize() == null ? 10L : query.getPageSize();
        // 排序：仅接受白名单内的逻辑字段名，未登记则回落默认排序（id ASC）
        String orderBy = SortWhitelist.resolve(query.getSortField(), query.getSortOrder(), SORT_WHITELIST);
        if (StringUtils.hasText(query.getSortField()) && orderBy == null) {
            log.warn("角色列表排序字段不在白名单，已忽略: sortField={}", query.getSortField());
        }
        IPage<SysAuthRole> page = roleMapper.selectRolePage(new Page<>(pageNum, pageSize), query, orderBy);
        List<RoleVO> list = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long createRole(RoleSaveRequest request, String operator) {
        // 角色 key 唯一性
        if (roleMapper.countByRoleKey(request.getRoleKey(), null) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "角色标识已存在: " + request.getRoleKey());
        }
        SysAuthRole role = new SysAuthRole();
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        role.setDataScope(StringUtils.hasText(request.getDataScope()) ? request.getDataScope() : "ALL");
        role.setCreatedBy(operator);
        role.setCreatedTime(LocalDateTime.now());
        try {
            roleMapper.insert(role);
        } catch (DuplicateKeyException e) {
            // 并发兜底：uk_role_key_active（V1.0.6）
            log.warn("并发创建同名角色被数据库唯一索引拒绝: roleKey={}", request.getRoleKey());
            throw new BusinessException(ResultCode.PARAM_ERROR, "角色标识已存在: " + request.getRoleKey());
        }
        log.info("新增角色成功: id={}, roleKey={}, operator={}", role.getId(), role.getRoleKey(), operator);
        return role.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleSaveRequest request, String operator) {
        SysAuthRole role = roleMapper.selectById(request.getId());
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在");
        }
        // 内置 admin 角色不可变更其 roleKey（保证超级角色语义不被破坏）
        if (isAdminRoleKey(role.getRoleKey()) && !role.getRoleKey().equals(request.getRoleKey())) {
            throw new BusinessException(ResultCode.BUILT_IN_DATA, "内置超级角色标识不可修改");
        }
        // key 变更时的唯一性校验
        if (!role.getRoleKey().equals(request.getRoleKey())
                && roleMapper.countByRoleKey(request.getRoleKey(), role.getId()) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "角色标识已存在: " + request.getRoleKey());
        }
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        role.setDataScope(StringUtils.hasText(request.getDataScope()) ? request.getDataScope() : "ALL");
        role.setUpdateBy(operator);
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateSelective(role);
        log.info("编辑角色成功: id={}, operator={}", role.getId(), operator);

        // 角色变更后清理持有该角色的用户缓存
        evictUsersOfRole(role.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id, String operator) {
        SysAuthRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在");
        }
        // 内置 admin 角色禁止删除
        if (isAdminRoleKey(role.getRoleKey())) {
            throw new BusinessException(ResultCode.BUILT_IN_DATA, "内置超级角色不可删除");
        }
        // 存在绑定用户时禁止删除（必须先解绑用户）
        if (userRoleMapper.countByRoleId(id) > 0) {
            throw new BusinessException(ResultCode.DATA_IN_USE, "该角色仍绑定用户，请先移除用户后再删除");
        }
        // 清理角色-权限关联（逻辑删除）
        rolePermissionMapper.logicDeleteByRoleId(id, operator);
        // 逻辑删除角色
        roleMapper.logicDelete(id, operator, LocalDateTime.now());
        log.info("删除角色成功: id={}, operator={}", id, operator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RolePermVO getRolePermissionTree(Long roleId) {
        SysAuthRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在");
        }
        // 已勾选权限 ID（启用项）
        List<Long> grantedIds = rolePermissionMapper.selectByRoleId(roleId).stream()
                .map(SysAuthRolePermission::getPermissionId)
                .distinct()
                .collect(Collectors.toList());

        RolePermVO vo = new RolePermVO();
        vo.setRoleId(roleId);
        vo.setPermissionIds(grantedIds);
        // 全量启用权限树（含菜单 + 按钮）
        List<PermissionVO> enabledNodes = new ArrayList<>();
        for (SysAuthPermission p : permissionMapper.selectAllOrdered()) {
            // 禁用项不允许被勾选（若历史已关联则保留勾选但树中隐藏）
            if (p.getStatus() == null || p.getStatus() == 0) {
                enabledNodes.add(toPermissionVO(p));
            }
        }
        vo.setPermissionTree(TreeUtils.buildTree(enabledNodes, true));
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(AssignPermissionRequest request, String operator) {
        Long roleId = request.getRoleId();
        SysAuthRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在");
        }
        List<Long> targetIds = request.getPermissionIds() == null
                ? Collections.emptyList()
                : request.getPermissionIds().stream().distinct().collect(Collectors.toList());

        // 1. 校验权限存在且启用（不存在的 ID 记录日志并忽略）
        List<Long> validIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(targetIds)) {
            List<SysAuthPermission> perms = permissionMapper.selectByIds(targetIds);
            for (SysAuthPermission perm : perms) {
                if (perm.getStatus() == null || perm.getStatus() == 0) {
                    validIds.add(perm.getId());
                }
            }
            // 被忽略的 ID（不存在或已禁用）必须留痕，否则前端传错会被静默吞掉、无从排查
            Set<Long> ignoredIds = new LinkedHashSet<>(targetIds);
            ignoredIds.removeAll(validIds);
            if (!ignoredIds.isEmpty()) {
                log.warn("分配角色权限时忽略无效或已禁用权限: roleId={}, ignoredIds={}, operator={}",
                        roleId, ignoredIds, operator);
            }
        }

        // 2. 旧关联逻辑删除 → 3. 插入新关联
        rolePermissionMapper.logicDeleteByRoleId(roleId, operator);
        LocalDateTime now = LocalDateTime.now();
        for (Long pid : validIds) {
            SysAuthRolePermission link = new SysAuthRolePermission();
            link.setRoleId(roleId);
            link.setPermissionId(pid);
            link.setCreatedBy(operator);
            link.setCreatedTime(now);
            rolePermissionMapper.insert(link);
        }
        log.info("分配角色权限成功: roleId={}, permissionCount={}, operator={}", roleId, validIds.size(), operator);

        // 4. 清理该角色用户的权限缓存（下次请求自动回源）
        evictUsersOfRole(roleId);
    }

    /* ------------------------------ 内部方法 ------------------------------ */

    /** 是否为内置 admin 角色 key */
    private boolean isAdminRoleKey(String roleKey) {
        return properties.getSecurity().getAdminRoleKey().equals(roleKey);
    }

    /** 清理持有指定角色的所有用户快照 */
    private void evictUsersOfRole(Long roleId) {
        // 受影响用户ID在事务内读取（含本次变更），真正的删除动作推迟到提交之后
        List<Long> userIds = userRbacService.listUserIdsByRoleIds(Collections.singletonList(roleId));
        afterCommitExecutor.run(() -> rbacCacheService.evictUsers(userIds));
    }

    /** 角色实体 → VO */
    private RoleVO toVO(SysAuthRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleKey(role.getRoleKey());
        vo.setDataScope(role.getDataScope());
        return vo;
    }

    /** 权限实体 → VO（children 由 TreeUtils 装配） */
    private PermissionVO toPermissionVO(SysAuthPermission p) {
        PermissionVO vo = new PermissionVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setParentId(p.getParentId());
        vo.setOrderNum(p.getOrderNum());
        vo.setType(p.getType());
        vo.setMenuUrl(p.getMenuUrl());
        vo.setComponent(p.getComponent());
        vo.setBackendUrl(p.getBackendUrl());
        vo.setStatus(p.getStatus());
        vo.setShow(p.getShow());
        vo.setIcon(p.getIcon());
        vo.setPermissionKey(p.getPermissionKey());
        return vo;
    }
}
