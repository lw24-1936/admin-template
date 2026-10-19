package com.admintem.web.service;

import com.admintem.web.entity.dto.DeptSaveRequest;
import com.admintem.web.entity.vo.DeptVO;

import java.util.List;

/**
 * 部门服务（树形组织）。
 *
 * <p>边界：一个用户只属于一个部门；部门本身不承载权限，
 * 只作为"人归属于哪里"的展示与筛选维度（数据权限不在本期范围）。</p>
 */
public interface SysAuthDeptService {

    /**
     * 查询部门树（含各部门用户数）。
     *
     * @return 顶级部门列表（children 递归装配）
     */
    List<DeptVO> treeList();

    /**
     * 新增部门。
     *
     * @param request 部门信息
     * @param operator 操作人
     * @return 新部门ID
     */
    Long createDept(DeptSaveRequest request, String operator);

    /**
     * 编辑部门。
     *
     * @param request 部门信息（id 必填）
     * @param operator 操作人
     */
    void updateDept(DeptSaveRequest request, String operator);

    /**
     * 删除部门（存在子部门或仍有用户时拒绝）。
     *
     * @param id 部门ID
     * @param operator 操作人
     */
    void deleteDept(Long id, String operator);
}
