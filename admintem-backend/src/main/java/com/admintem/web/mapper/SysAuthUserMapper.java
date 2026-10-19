package com.admintem.web.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.admintem.web.entity.SysAuthUser;
import com.admintem.web.entity.dto.UserPageQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户表 Mapper（sys_auth_user）。
 *
 * <p>本工程所有 SQL 均以 XML 方式维护（resources/mapper/SysAuthUserMapper.xml），
 * 接口仅声明方法与参数，便于审阅、复用与后续 SQL 调优。</p>
 */
public interface SysAuthUserMapper {

    /**
     * 按主键查询（自动过滤逻辑删除）。
     *
     * @param id 用户ID
     * @return 用户或 null
     */
    SysAuthUser selectById(@Param("id") Long id);

    /**
     * 按账号查询（登录/注册查重用）。
     *
     * @param userName 账号
     * @return 用户或 null
     */
    SysAuthUser selectByUserName(@Param("userName") String userName);

    /**
     * 批量按账号查询（在线会话页一次补齐昵称/状态，避免逐个查询造成 N+1）。
     *
     * @param userNames 账号集合（非空）
     * @return 用户列表（不存在的账号不会出现在结果里）
     */
    List<SysAuthUser> selectByUserNames(@Param("userNames") Collection<String> userNames);

    /**
     * 按部门分组统计用户数（部门树展示"这个部门有几个人"，避免逐个部门查）。
     *
     * @return 每项含 {@code deptId} 与 {@code total}
     */
    List<Map<String, Object>> countGroupByDept();

    /**
     * 统计同名账号数量（支持排除自身，用于唯一性校验）。
     *
     * @param userName  账号
     * @param excludeId 需要排除的用户ID（可为 null）
     * @return 数量
     */
    long countByUserName(@Param("userName") String userName, @Param("excludeId") Long excludeId);

    /**
     * 分页查询用户（多条件可选过滤）。
     *
     * @param page    MyBatis-Plus 分页对象（由分页插件自动拼接 LIMIT）
     * @param q       查询条件
     * @param orderBy 排序子句（形如 {@code user_name DESC}，由 SortWhitelist 依据白名单生成；
     *                为 null 时使用默认排序 created_time DESC, id DESC）
     * @return 分页结果
     */
    IPage<SysAuthUser> selectUserPage(IPage<SysAuthUser> page,
                                      @Param("q") UserPageQuery q,
                                      @Param("orderBy") String orderBy);

    /**
     * 新增用户（回填自增主键）。
     *
     * @param user 用户实体（created_* 由调用方填充，is_deleted 默认 0）
     * @return 影响行数
     */
    int insert(SysAuthUser user);

    /**
     * 选择性更新用户（仅更新非空字段，账号不可修改）。
     *
     * @param user 待更新字段（id 必填）
     * @return 影响行数
     */
    int updateSelective(SysAuthUser user);

    /**
     * 逻辑删除用户（is_deleted=1）。
     *
     * @param id         用户ID
     * @param updateBy   操作人
     * @param updateTime 操作时间
     * @return 影响行数
     */
    int logicDelete(@Param("id") Long id,
                    @Param("updateBy") String updateBy,
                    @Param("updateTime") LocalDateTime updateTime);
}
