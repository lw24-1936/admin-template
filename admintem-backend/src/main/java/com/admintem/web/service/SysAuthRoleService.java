package com.admintem.web.service;

import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.entity.dto.AssignPermissionRequest;
import com.admintem.web.entity.dto.RolePageQuery;
import com.admintem.web.entity.dto.RoleSaveRequest;
import com.admintem.web.entity.vo.RolePermVO;
import com.admintem.web.entity.vo.RoleVO;

import java.util.List;

/**
 * 角色管理服务。
 */
public interface SysAuthRoleService {

    /**
     * 查询全部角色（用于下拉/勾选选项）。
     *
     * @return 角色列表
     */
    List<RoleVO> listAll();

    /**
     * 分页查询角色。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<RoleVO> pageQuery(RolePageQuery query);

    /**
     * 新增角色。
     *
     * @param request  请求体
     * @param operator 操作人账号
     * @return 新角色ID
     */
    Long createRole(RoleSaveRequest request, String operator);

    /**
     * 编辑角色。
     *
     * @param request  请求体
     * @param operator 操作人账号
     */
    void updateRole(RoleSaveRequest request, String operator);

    /**
     * 删除角色（存在绑定用户时禁止删除）。
     *
     * @param id       角色ID
     * @param operator 操作人账号
     */
    void deleteRole(Long id, String operator);

    /**
     * 查询角色已分配权限 + 全量权限树（授权弹窗使用）。
     *
     * @param roleId 角色ID
     * @return 角色权限视图
     */
    RolePermVO getRolePermissionTree(Long roleId);

    /**
     * 全量覆盖分配角色权限。
     *
     * @param request  请求体
     * @param operator 操作人账号
     */
    void assignPermissions(AssignPermissionRequest request, String operator);
}
