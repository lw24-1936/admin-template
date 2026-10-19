package com.admintem.web.mapper;

import com.admintem.web.entity.SysAuthRolePermission;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 角色-权限关联表 Mapper（sys_auth_role_permission）。
 *
 * <p>SQL 见 resources/mapper/SysAuthRolePermissionMapper.xml。
 * 说明：关联表的“删除”均指逻辑删除（is_deleted=1），由本接口显式方法完成。</p>
 */
public interface SysAuthRolePermissionMapper {

    /** 查询某角色已分配的权限关联 */
    List<SysAuthRolePermission> selectByRoleId(@Param("roleId") Long roleId);

    /** 按角色 ID 集合查询关联（加载用户权限标识用） */
    List<SysAuthRolePermission> selectByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    /** 按权限 ID 查询关联（权限变更后清理缓存/角色用） */
    List<SysAuthRolePermission> selectByPermissionId(@Param("permissionId") Long permissionId);

    /** 按权限 ID 集合查询关联（删除权限级联清理用） */
    List<SysAuthRolePermission> selectByPermissionIds(@Param("permissionIds") Collection<Long> permissionIds);

    /** 新增一条角色-权限关联（is_deleted 默认 0） */
    int insert(SysAuthRolePermission relation);

    /** 逻辑删除某角色的全部关联（授权保存前清空） */
    int logicDeleteByRoleId(@Param("roleId") Long roleId, @Param("updateBy") String updateBy);

    /** 逻辑删除指向这些权限的全部关联（删除权限级联清理） */
    int logicDeleteByPermissionIds(@Param("permissionIds") Collection<Long> permissionIds, @Param("updateBy") String updateBy);
}
