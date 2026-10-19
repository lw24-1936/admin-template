package com.admintem.web.controller;

import com.admintem.web.aspect.AuditLog;
import com.admintem.web.aspect.AuditTargetType;
import com.admintem.web.common.Result;
import com.admintem.web.entity.dto.IdRequest;
import com.admintem.web.entity.dto.PermissionSaveRequest;
import com.admintem.web.entity.dto.validation.ValidationGroups;
import com.admintem.web.security.SecurityUtils;
import com.admintem.web.service.SysAuthPermissionService;
import com.admintem.web.entity.vo.PermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理控制器（sys_auth_permission，菜单 + 按钮）。
 *
 * <p>按钮级权限标识：新增 sysAuthPermission:addAuthPermission、
 * 编辑 sysAuthPermission:editAuthPermission、删除 sysAuthPermission:deleteById；
 * 树查询依赖"权限管理"菜单权限 userInfo:permissionInfo。</p>
 */
@Slf4j
@RestController
@RequestMapping("/sysAuthPermission")
@RequiredArgsConstructor
@Tag(name = "04-权限管理", description = "菜单与按钮权限的树形维护")
public class SysAuthPermissionController {

    private final SysAuthPermissionService permissionService;

    /**
     * 查询权限树（需要"权限管理"菜单权限）。
     *
     * @param includeDisabled 是否包含禁用项（默认 true，管理页需要看到全部）
     * @return 权限树（菜单节点下挂按钮子节点）
     */
    @GetMapping("/treeList")
    @PreAuthorize("@rbac.hasPerm('userInfo:permissionInfo')")
    @Operation(summary = "查询权限树")
    public Result<List<PermissionVO>> treeList(@RequestParam(defaultValue = "true") boolean includeDisabled) {
        return Result.ok(permissionService.treeAll(includeDisabled));
    }

    /**
     * 新增权限（菜单/按钮；需要按钮权限 sysAuthPermission:addAuthPermission）。
     */
    @PostMapping("/addAuthPermission")
    @AuditLog(action = "PERM_ADD", name = "新增权限", target = AuditTargetType.PERMISSION)
    @PreAuthorize("@rbac.hasPerm('sysAuthPermission:addAuthPermission')")
    @Operation(summary = "新增权限")
    public Result<Long> addAuthPermission(@Validated(ValidationGroups.Create.class) @RequestBody PermissionSaveRequest request) {
        return Result.ok(permissionService.createPermission(request, SecurityUtils.currentUserName()));
    }

    /**
     * 编辑权限（需要按钮权限 sysAuthPermission:editAuthPermission）。
     */
    @PostMapping("/editAuthPermission")
    @AuditLog(action = "PERM_EDIT", name = "编辑权限", target = AuditTargetType.PERMISSION)
    @PreAuthorize("@rbac.hasPerm('sysAuthPermission:editAuthPermission')")
    @Operation(summary = "编辑权限")
    public Result<Void> editAuthPermission(@Validated(ValidationGroups.Update.class) @RequestBody PermissionSaveRequest request) {
        permissionService.updatePermission(request, SecurityUtils.currentUserName());
        return Result.ok();
    }

    /**
     * 删除权限（级联删除子级与角色关联；需要按钮权限 sysAuthPermission:deleteById）。
     */
    @PostMapping("/deleteById")
    @AuditLog(action = "PERM_DELETE", name = "删除权限", target = AuditTargetType.PERMISSION)
    @PreAuthorize("@rbac.hasPerm('sysAuthPermission:deleteById')")
    @Operation(summary = "删除权限")
    public Result<Void> deleteById(@Valid @RequestBody IdRequest request) {
        permissionService.deletePermission(request.getId(), SecurityUtils.currentUserName());
        return Result.ok();
    }
}
