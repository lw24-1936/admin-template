package com.admintem.web.service.impl;

import com.admintem.web.common.ResultCode;
import com.admintem.web.entity.SysDept;
import com.admintem.web.entity.dto.DeptSaveRequest;
import com.admintem.web.entity.vo.DeptVO;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysAuthDeptMapper;
import com.admintem.web.mapper.SysAuthUserMapper;
import com.admintem.web.service.SysAuthDeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门服务实现。
 *
 * <p>建树方式：一次全量查询后在内存里按 parentId 组装（部门数量很小）。
 * 相比"每层递归查库"，既没有 N+1，也不会因为环数据无限递归——
 * 建树时只从 parentId=0 开始，环上的节点自然成为"不可达"，不会挂到树里。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAuthDeptServiceImpl implements SysAuthDeptService {

    private final SysAuthDeptMapper deptMapper;
    private final SysAuthUserMapper userMapper;

    @Override
    public List<DeptVO> treeList() {
        List<SysDept> all = deptMapper.selectAllOrdered();
        if (all == null || all.isEmpty()) {
            return Collections.emptyList();
        }
        // 1. 各直属部门的用户数（一次分组查询）
        Map<Long, Long> userCountByDept = loadUserCount();

        // 2. 转 VO 并按 parentId 分组
        Map<Long, DeptVO> voById = new LinkedHashMap<>();
        for (SysDept dept : all) {
            DeptVO vo = toVO(dept);
            vo.setUserCount(userCountByDept.getOrDefault(dept.getId(), 0L));
            voById.put(dept.getId(), vo);
        }

        // 3. 挂到父节点；父节点不存在（已删除/环数据）视为顶级，避免数据"消失"
        List<DeptVO> roots = new ArrayList<>();
        for (DeptVO vo : voById.values()) {
            Long parentId = vo.getParentId();
            DeptVO parent = parentId == null || parentId == SysDept.ROOT_PARENT_ID ? null : voById.get(parentId);
            if (parent == null) {
                roots.add(vo);
            } else {
                parent.getChildren().add(vo);
            }
        }
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDept(DeptSaveRequest request, String operator) {
        validateParent(request.getParentId(), null);
        if (deptMapper.countByDeptName(request.getDeptName(), null) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "部门名称已存在");
        }

        SysDept dept = new SysDept();
        dept.setParentId(request.getParentId());
        dept.setDeptName(request.getDeptName());
        dept.setOrderNum(request.getOrderNum() == null ? 1 : request.getOrderNum());
        dept.setLeader(request.getLeader());
        dept.setPhone(request.getPhone());
        dept.setStatus(request.getStatus() == null ? SysDept.STATUS_ENABLED : request.getStatus());
        dept.setCreatedBy(operator);
        dept.setCreatedTime(LocalDateTime.now());
        try {
            deptMapper.insert(dept);
        } catch (DuplicateKeyException e) {
            // 并发下"先查后插"仍可能撞唯一索引：统一转成同一条业务提示
            log.warn("部门名称并发冲突: deptName={}", request.getDeptName());
            throw new BusinessException(ResultCode.PARAM_ERROR, "部门名称已存在");
        }
        log.info("新增部门成功: id={}, name={}, parentId={}, operator={}",
                dept.getId(), dept.getDeptName(), dept.getParentId(), operator);
        return dept.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(DeptSaveRequest request, String operator) {
        SysDept exist = deptMapper.selectById(request.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "部门不存在");
        }
        if (request.getParentId() != null && request.getParentId().equals(request.getId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上级部门不能是自己");
        }
        validateParent(request.getParentId(), request.getId());
        if (request.getDeptName() != null
                && deptMapper.countByDeptName(request.getDeptName(), request.getId()) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "部门名称已存在");
        }

        SysDept update = new SysDept();
        update.setId(request.getId());
        update.setParentId(request.getParentId());
        update.setDeptName(request.getDeptName());
        update.setOrderNum(request.getOrderNum());
        update.setLeader(request.getLeader());
        update.setPhone(request.getPhone());
        update.setStatus(request.getStatus());
        update.setUpdateBy(operator);
        update.setUpdateTime(LocalDateTime.now());
        try {
            deptMapper.updateSelective(update);
        } catch (DuplicateKeyException e) {
            log.warn("部门名称并发冲突: deptName={}", request.getDeptName());
            throw new BusinessException(ResultCode.PARAM_ERROR, "部门名称已存在");
        }
        log.info("编辑部门成功: id={}, name={}, operator={}", request.getId(), request.getDeptName(), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long id, String operator) {
        SysDept exist = deptMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "部门不存在");
        }
        if (deptMapper.countChildren(id) > 0) {
            throw new BusinessException(ResultCode.DATA_IN_USE, "存在子部门，请先删除子部门");
        }
        Long userCount = countUsers(id);
        if (userCount != null && userCount > 0) {
            throw new BusinessException(ResultCode.DATA_IN_USE,
                    "该部门下还有 " + userCount + " 个用户，请先调整用户所属部门");
        }
        deptMapper.logicDelete(id, operator, LocalDateTime.now());
        log.info("删除部门成功: id={}, name={}, operator={}", id, exist.getDeptName(), operator);
    }

    /* ------------------------------ 内部方法 ------------------------------ */

    /**
     * 校验上级部门：必须存在且启用，并且不能把节点挂到自己的子孙下（成环）。
     *
     * @param parentId 目标上级ID（0 表示顶级，直接通过）
     * @param selfId   当前部门ID（新增时为 null）
     */
    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == SysDept.ROOT_PARENT_ID) {
            return;
        }
        SysDept parent = deptMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上级部门不存在");
        }
        if (parent.getStatus() != null && parent.getStatus() == SysDept.STATUS_DISABLED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上级部门已禁用，不能挂载");
        }
        if (selfId == null) {
            return;
        }
        // 从目标上级一路向上找，遇到自己说明要成环
        Long cursor = parentId;
        int guard = 0;
        while (cursor != null && cursor != SysDept.ROOT_PARENT_ID) {
            if (cursor.equals(selfId)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "不能把部门移动到自己的下级");
            }
            SysDept node = deptMapper.selectById(cursor);
            if (node == null) {
                break;
            }
            cursor = node.getParentId();
            // 防御脏数据造成的死循环（层级超过 50 视为异常）
            if (++guard > 50) {
                log.warn("部门层级异常（疑似成环）: selfId={}, parentId={}", selfId, parentId);
                throw new BusinessException(ResultCode.PARAM_ERROR, "部门层级异常，请检查上级设置");
            }
        }
    }

    /** 统计各部门用户数（按 dept_id 分组） */
    private Map<Long, Long> loadUserCount() {
        List<Map<String, Object>> rows = userMapper.countGroupByDept();
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object deptId = row.get("deptId");
            Object total = row.get("total");
            if (deptId instanceof Number && total instanceof Number) {
                result.put(((Number) deptId).longValue(), ((Number) total).longValue());
            }
        }
        return result;
    }

    /** 某部门下的用户数 */
    private Long countUsers(Long deptId) {
        Map<Long, Long> counts = loadUserCount();
        return counts.getOrDefault(deptId, 0L);
    }

    /** 实体 → VO */
    private DeptVO toVO(SysDept dept) {
        DeptVO vo = new DeptVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setDeptName(dept.getDeptName());
        vo.setOrderNum(dept.getOrderNum());
        vo.setLeader(dept.getLeader());
        vo.setPhone(dept.getPhone());
        vo.setStatus(dept.getStatus());
        return vo;
    }
}
