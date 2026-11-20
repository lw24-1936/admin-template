package com.admintem.web.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.admintem.web.entity.SysAuthAuditLog;
import com.admintem.web.entity.dto.AuditLogPageQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作审计日志 Mapper（sys_auth_audit_log）。
 *
 * <p>写操作提供 {@link #insert} 与仅由保留期任务调用的 {@link #deleteBefore}；
 * 业务代码不得删除审计记录，避免误改历史留痕。</p>
 *
 * <p>读操作提供分页查询与动作字典：查询条件与索引一一对应
 * （operator / action / created_time 均有联合索引，见 V1.0.5 建表脚本），
 * 因此"按操作人 + 时间倒序"这类常用查询不会退化成全表扫描。</p>
 *
 * <p>SQL 见 resources/mapper/SysAuthAuditLogMapper.xml。</p>
 */
public interface SysAuthAuditLogMapper {

    /**
     * 新增一条审计日志。
     *
     * @param auditLog 审计记录
     * @return 影响行数
     */
    int insert(SysAuthAuditLog auditLog);

    /**
     * 删除早于指定时间的审计记录（仅保留期任务使用；单批上限避免长事务）。
     *
     * @param before 截止时间（不含）
     * @param limit  单批最多删除行数
     * @return 实际删除行数
     */
    int deleteBefore(@Param("before") LocalDateTime before, @Param("limit") int limit);

    /**
     * 分页查询审计日志（按发生时间倒序，同一毫秒内按 id 倒序保证稳定）。
     *
     * @param page  分页参数
     * @param query 过滤条件（各字段可为空）
     * @return 分页结果
     */
    IPage<SysAuthAuditLog> selectAuditPage(IPage<SysAuthAuditLog> page,
                                          @Param("q") AuditLogPageQuery query);

    /**
     * 动作字典：已出现过的动作码与中文名（供前端筛选下拉使用）。
     *
     * <p>用 GROUP BY 而不是 SELECT DISTINCT：同一动作的中文名理论上唯一，
     * 但历史数据里若出现重命名，GROUP BY 能一并带出（前端取第一条即可）。</p>
     *
     * @return 每项含 action / actionName / total
     */
    List<Map<String, Object>> selectActionDict();
}
