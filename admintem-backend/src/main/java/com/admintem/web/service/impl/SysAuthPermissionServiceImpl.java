package com.admintem.web.service.impl;

import com.admintem.web.common.ResultCode;
import com.admintem.web.entity.SysAuthPermission;
import com.admintem.web.entity.SysAuthRolePermission;
import com.admintem.web.entity.dto.PermissionSaveRequest;
import com.admintem.web.entity.vo.PermissionVO;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysAuthPermissionMapper;
import com.admintem.web.mapper.SysAuthRolePermissionMapper;
import com.admintem.web.security.RbacCacheService;
import com.admintem.web.service.SysAuthPermissionService;
import com.admintem.web.service.UserRbacService;
import com.admintem.web.support.AfterCommitExecutor;
import com.admintem.web.util.tree.TreeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限（菜单/按钮）管理服务实现（数据访问统一走 Mapper XML）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAuthPermissionServiceImpl implements SysAuthPermissionService {

    private final SysAuthPermissionMapper permissionMapper;
    private final SysAuthRolePermissionMapper rolePermissionMapper;
    private final UserRbacService userRbacService;
    private final RbacCacheService rbacCacheService;
    /** 缓存失效统一挂到事务提交之后（见 AfterCommitExecutor 的时序说明） */
    private final AfterCommitExecutor afterCommitExecutor;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PermissionVO> treeAll(boolean includeDisabled) {
        // 全量查询（XML 已过滤逻辑删除），按需过滤禁用项后组树
        List<PermissionVO> nodes = permissionMapper.selectAllOrdered().stream()
                .filter(p -> includeDisabled || isEnabled(p))
                .map(this::toVO)
                .collect(Collectors.toList());
        // 孤儿节点提升为根，防止权限维护过程中出现临时结构时节点丢失
        return TreeUtils.buildTree(nodes, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPermission(PermissionSaveRequest request, String operator) {
        // 1. 权限标识唯一性校验
        if (permissionMapper.countByKey(request.getPermissionKey(), null) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "权限标识已存在: " + request.getPermissionKey());
        }
        // 2. 父级存在性校验（新增无自身 ID，不做回路校验）
        validateParent(request.getParentId(), null);

        SysAuthPermission entity = new SysAuthPermission();
        copyToEntity(request, entity);
        entity.setCreatedBy(operator);
        entity.setCreatedTime(LocalDateTime.now());
        // 默认启用
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        // 未指定排序号时取同级最大排序号 + 1
        if (entity.getOrderNum() == null) {
            entity.setOrderNum(nextOrder(request.getParentId()));
        }
        try {
            permissionMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 并发兜底：uk_permission_key_active（V1.0.6）
            log.warn("并发创建同名权限被数据库唯一索引拒绝: permissionKey={}", request.getPermissionKey());
            throw new BusinessException(ResultCode.PARAM_ERROR, "权限标识已存在: " + request.getPermissionKey());
        }
        log.info("新增权限成功: id={}, key={}, operator={}", entity.getId(), entity.getPermissionKey(), operator);
        return entity.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(PermissionSaveRequest request, String operator) {
        SysAuthPermission entity = permissionMapper.selectById(request.getId());
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "权限不存在");
        }
        // 父级不能是自己
        if (request.getParentId() != null && request.getParentId().equals(entity.getId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "父级不能是自己");
        }
        // 权限标识唯一性（排除自身）
        if (!request.getPermissionKey().equals(entity.getPermissionKey())
                && permissionMapper.countByKey(request.getPermissionKey(), entity.getId()) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "权限标识已存在: " + request.getPermissionKey());
        }
        validateParent(request.getParentId(), entity.getId());

        copyToEntity(request, entity);
        entity.setUpdateBy(operator);
        entity.setUpdateTime(LocalDateTime.now());
        permissionMapper.updateSelective(entity);
        log.info("编辑权限成功: id={}, operator={}", entity.getId(), operator);

        // 权限标识/状态变化会影响所有持有该权限的用户 → 清理其缓存快照
        evictUsersByPermissionId(entity.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Long id, String operator) {
        SysAuthPermission entity = permissionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "权限不存在");
        }
        // 1. 级联收集本节点 + 全部子孙节点
        List<Long> cascadeIds = new ArrayList<>();
        cascadeIds.add(id);
        collectChildren(id, cascadeIds);

        // 2. 清理这些权限的角色关联（逻辑删除），并记录受影响角色
        List<Long> affectedRoleIds = deleteRolePermissionLinks(cascadeIds, operator);

        // 3. 逻辑删除权限节点
        LocalDateTime now = LocalDateTime.now();
        permissionMapper.logicDeleteByIds(cascadeIds, operator, now);
        log.info("删除权限成功: id={}, cascadeIds={}, operator={}", id, cascadeIds, operator);

        // 4. 清理受影响用户的缓存快照
        evictUsersByRoleIds(affectedRoleIds);
    }

    /* ------------------------------ 内部方法 ------------------------------ */

    /** 权限是否启用（status 为空按启用处理，兼容历史数据） */
    private boolean isEnabled(SysAuthPermission p) {
        return p.getStatus() == null || p.getStatus() == 0;
    }

    /**
     * 校验父级存在，且父级不能是自己的子孙（防止父子成环）。
     *
     * <p>一旦成环：{@link TreeUtils#buildTree} 会把环内节点全部判为非根节点，
     * 使它们在权限树/菜单树中静默消失（界面看不到也就改不回来）；
     * {@link #collectChildren} 递归收集子级时还会无限递归导致 StackOverflowError。</p>
     *
     * @param parentId 目标父级 ID（null 或 0 表示顶级，不做父级校验）
     * @param selfId   当前节点 ID（新增时传 null，此时不存在回路问题）
     */
    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (permissionMapper.selectById(parentId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "父级权限不存在: " + parentId);
        }
        if (selfId == null) {
            return;
        }
        // 自目标父级向上回溯；visited 兜底历史上已存在的环，避免死循环
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0L && visited.add(cursor)) {
            if (cursor.equals(selfId)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "父级不能是自己的子级");
            }
            SysAuthPermission parent = permissionMapper.selectById(cursor);
            cursor = parent == null ? null : parent.getParentId();
        }
    }

    /** 计算同级下一个排序号 */
    private int nextOrder(Long parentId) {
        return permissionMapper.selectByParentId(parentId == null ? 0L : parentId).stream()
                .map(SysAuthPermission::getOrderNum)
                .filter(o -> o != null)
                .max(Integer::compareTo)
                .map(o -> o + 1)
                .orElse(1);
    }

    /** 请求体 → 实体字段拷贝（仅业务字段） */
    private void copyToEntity(PermissionSaveRequest req, SysAuthPermission entity) {
        entity.setName(req.getName());
        entity.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        entity.setOrderNum(req.getOrderNum());
        entity.setType(req.getType());
        entity.setMenuUrl(req.getMenuUrl());
        entity.setComponent(req.getComponent());
        entity.setBackendUrl(req.getBackendUrl());
        entity.setStatus(req.getStatus());
        entity.setShow(req.getShow());
        entity.setIcon(req.getIcon());
        entity.setPermissionKey(req.getPermissionKey());
    }

    /** 实体 → VO（children 由 TreeUtils 装配） */
    private PermissionVO toVO(SysAuthPermission p) {
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

    /**
     * 收集子节点 ID（一次性查全表后在内存遍历，避免按层递归查库的 N+1）。
     *
     * <p>原实现每层调用一次 {@code selectByParentId}：删除一个含 N 层子树的节点会执行
     * N 次及以上查询，深树时明显拖慢响应。权限总量本身很小（当前 18 条），
     * 因此改为「一次全表查询 + 内存父子索引 + 迭代遍历」，SQL 条数由 N 降为 1。</p>
     *
     * @param parentId  起始节点 ID
     * @param collector 收集结果（含全部子孙，不含起始节点自身）
     */
    private void collectChildren(Long parentId, List<Long> collector) {
        Map<Long, List<Long>> childrenIndex = permissionMapper.selectAllOrdered().stream()
                .filter(p -> p.getParentId() != null)
                .collect(Collectors.groupingBy(SysAuthPermission::getParentId,
                        Collectors.mapping(SysAuthPermission::getId, Collectors.toList())));
        // 迭代 + 已访问集合：既避免递归深度问题，也兜底历史上可能存在的父子环
        Set<Long> visited = new HashSet<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.add(parentId);
        while (!pending.isEmpty()) {
            Long current = pending.poll();
            for (Long childId : childrenIndex.getOrDefault(current, Collections.emptyList())) {
                if (visited.add(childId)) {
                    collector.add(childId);
                    pending.add(childId);
                }
            }
        }
    }

    /**
     * 逻辑删除这些权限的角色关联，返回受影响角色 ID。
     *
     * @param permissionIds 权限 ID 集合（含子孙节点）
     * @param operator      操作人（写入关联表 update_by，便于审计"谁在何时收回了权限"）
     * @return 受影响的角色 ID（已去重）
     */
    private List<Long> deleteRolePermissionLinks(List<Long> permissionIds, String operator) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return Collections.emptyList();
        }
        // 先记录受影响角色，再逻辑删除关联
        List<Long> roleIds = rolePermissionMapper.selectByPermissionIds(permissionIds).stream()
                .map(SysAuthRolePermission::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        rolePermissionMapper.logicDeleteByPermissionIds(permissionIds, operator);
        return roleIds;
    }

    /** 清理拥有指定权限的用户快照 */
    private void evictUsersByPermissionId(Long permissionId) {
        List<Long> roleIds = rolePermissionMapper.selectByPermissionId(permissionId).stream()
                .map(SysAuthRolePermission::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        evictUsersByRoleIds(roleIds);
    }

    /** 清理拥有这些角色的用户快照（受影响的用户ID在事务内读出，删除动作延迟到提交后） */
    private void evictUsersByRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        List<Long> userIds = userRbacService.listUserIdsByRoleIds(roleIds);
        afterCommitExecutor.run(() -> rbacCacheService.evictUsers(userIds));
    }
}
