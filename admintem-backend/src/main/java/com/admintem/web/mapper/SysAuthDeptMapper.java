package com.admintem.web.mapper;

import com.admintem.web.entity.SysDept;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 部门 Mapper（sys_dept）。
 *
 * <p>SQL 全部在 resources/mapper/SysAuthDeptMapper.xml，与本工程其它 Mapper 一致。</p>
 */
public interface SysAuthDeptMapper {

    /**
     * 查询全部未删除部门（按 parent_id, order_num, id 排序，便于内存建树）。
     *
     * @return 部门列表
     */
    List<SysDept> selectAllOrdered();

    /**
     * 按 ID 查询。
     *
     * @param id 部门ID
     * @return 部门或 null
     */
    SysDept selectById(@Param("id") Long id);

    /**
     * 按 ID 批量查询（用户列表补部门名用）。
     *
     * @param ids ID 集合
     * @return 部门列表
     */
    List<SysDept> selectByIds(@Param("ids") Collection<Long> ids);

    /**
     * 统计某部门的直属子部门数（判断能否删除）。
     *
     * @param parentId 部门ID
     * @return 子部门数
     */
    long countChildren(@Param("parentId") Long parentId);

    /**
     * 统计某部门名称是否已存在（名称全局唯一）。
     *
     * @param deptName 部门名称
     * @param excludeId 需排除的部门ID（编辑时传自身，新增传 null）
     * @return 命中数
     */
    long countByDeptName(@Param("deptName") String deptName, @Param("excludeId") Long excludeId);

    /**
     * 新增部门。
     *
     * @param dept 部门
     * @return 影响行数
     */
    int insert(SysDept dept);

    /**
     * 选择性更新（只更新非空字段）。
     *
     * @param dept 部门
     * @return 影响行数
     */
    int updateSelective(SysDept dept);

    /**
     * 逻辑删除。
     *
     * @param id       部门ID
     * @param updateBy 操作人
     * @param now      操作时间
     * @return 影响行数
     */
    int logicDelete(@Param("id") Long id, @Param("updateBy") String updateBy, @Param("now") LocalDateTime now);
}
