package com.admintem.web.mapper;

import com.admintem.web.entity.SysAuthUserRole;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 用户-角色关联表 Mapper（sys_auth_user_role）。
 *
 * <p>SQL 见 resources/mapper/SysAuthUserRoleMapper.xml。
 * 说明：关联表的“删除”均指逻辑删除（is_deleted=1），由本接口显式方法完成。</p>
 */
public interface SysAuthUserRoleMapper {

    /** 查询用户已分配的角色 ID 列表 */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /** 按用户 ID 集合查询全部关联（列表页批量装载角色用） */
    List<SysAuthUserRole> selectByUserIds(@Param("userIds") Collection<Long> userIds);

    /** 统计某角色当前绑定的用户数（删除角色前的占用检查） */
    long countByRoleId(@Param("roleId") Long roleId);

    /** 反查拥有这些角色的用户 ID（去重，用于权限变更后清理缓存） */
    List<Long> selectUserIdsByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    /** 新增一条用户-角色关联（is_deleted 默认 0） */
    int insert(SysAuthUserRole relation);

    /** 逻辑删除某用户的全部关联 */
    int logicDeleteByUserId(@Param("userId") Long userId, @Param("updateBy") String updateBy);

    /** 逻辑删除某角色的全部关联 */
    int logicDeleteByRoleId(@Param("roleId") Long roleId, @Param("updateBy") String updateBy);
}
