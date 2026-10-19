package com.admintem.web.mapper;

import com.admintem.web.entity.SysAuthPermission;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 权限表 Mapper（sys_auth_permission，菜单 + 按钮）。
 *
 * <p>SQL 见 resources/mapper/SysAuthPermissionMapper.xml。</p>
 */
public interface SysAuthPermissionMapper {

    /** 按主键查询（自动过滤逻辑删除） */
    SysAuthPermission selectById(@Param("id") Long id);

    /** 按 ID 集合批量查询（自动过滤逻辑删除） */
    List<SysAuthPermission> selectByIds(@Param("ids") Collection<Long> ids);

    /**
     * 按 permission_key 集合批量查询（自动过滤逻辑删除）。
     *
     * <p>会话构建用：已认证快照里就带着权限标识，直接按标识取权限行装配菜单树，
     * 避免"权限表全表扫描 + 内存过滤"或重复查角色关联。</p>
     */
    List<SysAuthPermission> selectByKeys(@Param("permissionKeys") Collection<String> permissionKeys);

    /** 查询全部权限（order_num、id 升序；权限树/启用 key 集合均基于此结果过滤） */
    List<SysAuthPermission> selectAllOrdered();

    /** 按父级 ID 查询直接子级（删除级联收集用） */
    List<SysAuthPermission> selectByParentId(@Param("parentId") Long parentId);

    /** 统计同 permission_key 数量（支持排除自身） */
    long countByKey(@Param("key") String permissionKey, @Param("excludeId") Long excludeId);

    /** 新增权限（回填自增主键） */
    int insert(SysAuthPermission permission);

    /** 选择性更新权限（仅更新非空字段） */
    int updateSelective(SysAuthPermission permission);

    /** 批量逻辑删除权限（is_deleted=1） */
    int logicDeleteByIds(@Param("ids") Collection<Long> ids,
                         @Param("updateBy") String updateBy,
                         @Param("updateTime") LocalDateTime updateTime);
}
