package com.admintem.web.controller;

import com.admintem.web.aspect.AuditLog;
import com.admintem.web.aspect.AuditTargetType;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.common.Result;
import com.admintem.web.entity.dto.AssignPermissionRequest;
import com.admintem.web.entity.dto.IdRequest;
import com.admintem.web.entity.dto.RolePageQuery;
import com.admintem.web.entity.dto.RoleSaveRequest;
import com.admintem.web.entity.dto.validation.ValidationGroups;
import com.admintem.web.security.SecurityUtils;
import com.admintem.web.service.SysAuthRoleService;
import com.admintem.web.entity.vo.RoleVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理控制器（sys_auth_role）。
 *
 * <p>按钮级权限标识：
 * 新增 sysAuthRole:addAuthRole、编辑 sysAuthRole:editAuthRole、
 * 删除 sysAuthRole:deleteById、分配权限 sysAuthRole:assignPermission；
 * 列表查询依赖"角色管理"菜单权限 userInfo:roleInfo。</p>
 */
@Slf4j
@RestController
@RequestMapping("/sysAuthRole")
@RequiredArgsConstructor
@Tag(name = "03-角色管理", description = "角色 CRUD 与权限分配")
public class SysAuthRoleController {

    private final SysAuthRoleService sysAuthRoleService;

    /**
     * 查询全部角色（下拉选项使用；需要用户管理或角色管理菜单权限）。
     *
     * @return 角色列表
     */
    @GetMapping("/queryAll")
    @PreAuthorize("@rbac.hasAnyPerm('userInfo:userInfo', 'userInfo:roleInfo', 'userInfo:permissionInfo')")
    @Operation(summary = "查询全部角色")
    public Result<List<RoleVO>> queryAll() {
        return Result.ok(sysAuthRoleService.listAll());
    }

    /**
     * 分页查询角色（需要"角色管理"菜单权限）。
     *
     * <p>加 @Valid 才能真正启用 PageQuery 上的 pageNum/pageSize 边界约束，
     * 否则 @Min/@Max 只是无效注解。</p>
     *
     * @param query 查询条件（pageNum ≥ 1，pageSize 1~500）
     * @return 分页角色
     */
    @PostMapping("/paginQuery")
    @PreAuthorize("@rbac.hasPerm('userInfo:roleInfo')")
    @Operation(summary = "分页查询角色")
    public Result<PageResult<RoleVO>> paginQuery(@Valid @RequestBody RolePageQuery query) {
        return Result.ok(sysAuthRoleService.pageQuery(query));
    }

    /**
     * 新增角色（需要按钮权限 sysAuthRole:addAuthRole）。
     */
    @PostMapping("/addAuthRole")
    @AuditLog(action = "ROLE_ADD", name = "新增角色", target = AuditTargetType.ROLE)
    @PreAuthorize("@rbac.hasPerm('sysAuthRole:addAuthRole')")
    @Operation(summary = "新增角色")
    public Result<Long> addAuthRole(@Validated(ValidationGroups.Create.class) @RequestBody RoleSaveRequest request) {
        return Result.ok(sysAuthRoleService.createRole(request, SecurityUtils.currentUserName()));
    }

    /**
     * 编辑角色（需要按钮权限 sysAuthRole:editAuthRole）。
     *
     * <p>使用 Update 校验分组：在默认约束之外额外要求 id 非空，
     * 缺 id 时直接返回"ID不能为空"，而不是落到业务层的"数据不存在"。</p>
     */
    @PostMapping("/editAuthRole")
    @AuditLog(action = "ROLE_EDIT", name = "编辑角色", target = AuditTargetType.ROLE)
    @PreAuthorize("@rbac.hasPerm('sysAuthRole:editAuthRole')")
    @Operation(summary = "编辑角色")
    public Result<Void> editAuthRole(@Validated(ValidationGroups.Update.class) @RequestBody RoleSaveRequest request) {
        sysAuthRoleService.updateRole(request, SecurityUtils.currentUserName());
        return Result.ok();
    }

    /**
     * 删除角色（需要按钮权限 sysAuthRole:deleteById）。
     */
    @PostMapping("/deleteById")
    @AuditLog(action = "ROLE_DELETE", name = "删除角色", target = AuditTargetType.ROLE)
    @PreAuthorize("@rbac.hasPerm('sysAuthRole:deleteById')")
    @Operation(summary = "删除角色")
    public Result<Void> deleteById(@Valid @RequestBody IdRequest request) {
        sysAuthRoleService.deleteRole(request.getId(), SecurityUtils.currentUserName());
        return Result.ok();
    }

    /**
     * 全量分配角色权限（需要按钮权限 sysAuthRole:assignPermission）。
     *
     * @param request 角色 ID + 权限 ID 列表
     * @return 成功提示
     */
    @PostMapping("/assignPermission")
    @AuditLog(action = "ROLE_ASSIGN_PERM", name = "分配角色权限", target = AuditTargetType.ROLE)
    @PreAuthorize("@rbac.hasPerm('sysAuthRole:assignPermission')")
    @Operation(summary = "分配角色权限")
    public Result<Void> assignPermission(@Valid @RequestBody AssignPermissionRequest request) {
        sysAuthRoleService.assignPermissions(request, SecurityUtils.currentUserName());
        return Result.ok();
    }
}
