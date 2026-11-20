package com.admintem.web.controller;

import com.admintem.web.aspect.AuditLog;
import com.admintem.web.aspect.AuditTargetType;
import com.admintem.web.common.Result;
import com.admintem.web.entity.dto.ConfigPageQuery;
import com.admintem.web.entity.dto.ConfigSaveRequest;
import com.admintem.web.entity.dto.IdRequest;
import com.admintem.web.entity.dto.validation.ValidationGroups;
import com.admintem.web.entity.vo.ConfigVO;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.security.SecurityUtils;
import com.admintem.web.service.SysAuthConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统参数配置控制器。
 *
 * <p>权限：菜单 {@code sysConfig:sysConfig}，按钮 {@code sysConfig:add/edit/delete}。</p>
 */
@Slf4j
@RestController
@RequestMapping("/sysConfig")
@RequiredArgsConstructor
@Tag(name = "07-系统参数", description = "系统级键值参数配置")
public class SysAuthConfigController {

    private final SysAuthConfigService configService;

    @PostMapping("/paginQuery")
    @PreAuthorize("@rbac.hasPerm('sysConfig:sysConfig')")
    @Operation(summary = "分页查询系统参数")
    public Result<PageResult<ConfigVO>> paginQuery(@Valid @RequestBody ConfigPageQuery query) {
        return Result.ok(configService.pageQuery(query));
    }

    @PostMapping("/add")
    @AuditLog(action = "CONFIG_ADD", name = "新增系统参数", target = AuditTargetType.CONFIG)
    @PreAuthorize("@rbac.hasPerm('sysConfig:add')")
    @Operation(summary = "新增系统参数")
    public Result<Long> add(@Validated(ValidationGroups.Create.class) @RequestBody ConfigSaveRequest request) {
        return Result.ok(configService.create(request, SecurityUtils.currentUserName()));
    }

    @PostMapping("/edit")
    @AuditLog(action = "CONFIG_EDIT", name = "编辑系统参数", target = AuditTargetType.CONFIG)
    @PreAuthorize("@rbac.hasPerm('sysConfig:edit')")
    @Operation(summary = "编辑系统参数")
    public Result<Void> edit(@Validated(ValidationGroups.Update.class) @RequestBody ConfigSaveRequest request) {
        configService.update(request, SecurityUtils.currentUserName());
        return Result.ok();
    }

    @PostMapping("/deleteById")
    @AuditLog(action = "CONFIG_DELETE", name = "删除系统参数", target = AuditTargetType.CONFIG)
    @PreAuthorize("@rbac.hasPerm('sysConfig:delete')")
    @Operation(summary = "删除系统参数")
    public Result<Void> deleteById(@Valid @RequestBody IdRequest request) {
        configService.delete(request.getId(), SecurityUtils.currentUserName());
        return Result.ok();
    }
}
