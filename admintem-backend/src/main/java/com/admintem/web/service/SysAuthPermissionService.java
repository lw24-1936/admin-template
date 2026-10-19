package com.admintem.web.service;

import com.admintem.web.entity.dto.PermissionSaveRequest;
import com.admintem.web.entity.vo.PermissionVO;

import java.util.List;

/**
 * 权限（菜单/按钮）管理服务。
 */
public interface SysAuthPermissionService {

    /**
     * 查询全部权限树。
     *
     * @param includeDisabled 是否包含禁用项（管理页 true；授权选择一般 false）
     * @return 权限树
     */
    List<PermissionVO> treeAll(boolean includeDisabled);

    /**
     * 新增权限（菜单或按钮）。
     *
     * @param request  请求体
     * @param operator 操作人账号
     * @return 新权限ID
     */
    Long createPermission(PermissionSaveRequest request, String operator);

    /**
     * 编辑权限。
     *
     * @param request  请求体
     * @param operator 操作人账号
     */
    void updatePermission(PermissionSaveRequest request, String operator);

    /**
     * 删除权限：自动级联删除其子级与角色关联。
     *
     * @param id       权限ID
     * @param operator 操作人账号
     */
    void deletePermission(Long id, String operator);
}
