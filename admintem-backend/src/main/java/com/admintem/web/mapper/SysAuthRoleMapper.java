package com.admintem.web.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.admintem.web.entity.SysAuthRole;
import com.admintem.web.entity.dto.RolePageQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 角色表 Mapper（sys_auth_role）。
 *
 * <p>SQL 见 resources/mapper/SysAuthRoleMapper.xml。</p>
 */
public interface SysAuthRoleMapper {

    /** 按主键查询（自动过滤逻辑删除） */
    SysAuthRole selectById(@Param("id") Long id);

    /** 按 ID 集合批量查询（自动过滤逻辑删除，返回顺序与传入无关） */
    List<SysAuthRole> selectByIds(@Param("ids") Collection<Long> ids);

    /** 按 role_key 查询（取第一条，用于注册默认角色等场景） */
    SysAuthRole selectByRoleKey(@Param("roleKey") String roleKey);

    /** 统计同名 role_key 数量（支持排除自身） */
    long countByRoleKey(@Param("roleKey") String roleKey, @Param("excludeId") Long excludeId);

    /** 按 role_key 集合批量查询（会话构建复用已认证快照的角色标识，省去两次查询） */
    List<SysAuthRole> selectByRoleKeys(@Param("roleKeys") Collection<String> roleKeys);

    /** 查询全部角色（id 升序，供下拉选项） */
    List<SysAuthRole> selectAllOrdered();

    /**
     * 分页查询角色（roleName/roleKey 模糊过滤）。
     *
     * @param page    MyBatis-Plus 分页对象
     * @param q       查询条件
     * @param orderBy 排序子句（由 SortWhitelist 依据白名单生成；为 null 时默认 id ASC）
     * @return 分页结果
     */
    IPage<SysAuthRole> selectRolePage(IPage<SysAuthRole> page,
                                      @Param("q") RolePageQuery q,
                                      @Param("orderBy") String orderBy);

    /** 新增角色（回填自增主键） */
    int insert(SysAuthRole role);

    /** 选择性更新角色（仅更新非空字段） */
    int updateSelective(SysAuthRole role);

    /** 逻辑删除角色 */
    int logicDelete(@Param("id") Long id,
                    @Param("updateBy") String updateBy,
                    @Param("updateTime") LocalDateTime updateTime);
}
