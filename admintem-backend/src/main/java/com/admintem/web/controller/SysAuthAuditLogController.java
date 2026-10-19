package com.admintem.web.controller;

import com.admintem.web.common.Result;
import com.admintem.web.entity.dto.AuditLogPageQuery;
import com.admintem.web.entity.vo.AuditLogVO;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 操作审计控制器（只读）。
 *
 * <p>审计表由切面异步写入（见 {@code AuditLogAspect}），本控制器只负责"看"：</p>
 * <ul>
 *   <li>{@code /paginQuery}：按操作人/动作/目标/结果/时间范围分页查询；</li>
 *   <li>{@code /queryActions}：动作字典，供前端筛选下拉使用。</li>
 * </ul>
 *
 * <p>权限：页面入口需要菜单权限 {@code auditLog:auditLog}，
 * 查询接口需要按钮权限 {@code sysAuthAuditLog:paginQuery}（与前端 {@code v-perm} 同一标识）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/sysAuthAuditLog")
@RequiredArgsConstructor
@Tag(name = "06-操作审计", description = "操作留痕查询（只读）")
public class SysAuthAuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 分页查询审计记录。
     *
     * <p>排序固定为"发生时间倒序、同时间按 id 倒序"，不接受前端指定排序字段：
     * 审计表的可排序维度有限，且固定排序才能稳定利用 {@code created_time} 索引。</p>
     *
     * @param query 过滤条件（分页 + 操作人 + 动作 + 目标 + 结果 + 时间范围）
     * @return 分页审计记录
     */
    @PostMapping("/paginQuery")
    @PreAuthorize("@rbac.hasPerm('sysAuthAuditLog:paginQuery')")
    @Operation(summary = "分页查询操作审计")
    public Result<PageResult<AuditLogVO>> paginQuery(@Valid @RequestBody AuditLogPageQuery query) {
        return Result.ok(auditLogService.pageQuery(query));
    }

    /**
     * 动作字典：已出现过的动作码与中文名（按出现次数倒序）。
     *
     * <p>查询接口与字典共用同一按钮权限：能查审计的人才会需要筛选下拉。</p>
     *
     * @return 动作列表
     */
    @PostMapping("/queryActions")
    @PreAuthorize("@rbac.hasPerm('sysAuthAuditLog:paginQuery')")
    @Operation(summary = "查询动作字典")
    public Result<List<Map<String, Object>>> queryActions() {
        return Result.ok(auditLogService.queryActionDict());
    }
}
