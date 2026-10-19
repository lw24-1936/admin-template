package com.admintem.web.controller;

import com.admintem.web.common.Result;
import com.admintem.web.entity.dto.RoleIdRequest;
import com.admintem.web.service.SysAuthRoleService;
import com.admintem.web.entity.vo.RolePermVO;
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

/**
 * 角色-权限关系查询控制器。
 *
 * <p>查询角色已勾选的权限 ID 与全量权限树（角色管理页的"授权"弹窗需要）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/sysAuthRolePermission")
@RequiredArgsConstructor
@Tag(name = "03B-角色权限查询", description = "查询角色已分配权限")
public class SysAuthRolePermissionController {

    private final SysAuthRoleService sysAuthRoleService;

    /**
     * 根据角色 ID 查询其权限与全量权限树（需要"角色管理"菜单权限）。
     *
     * @param request 角色ID
     * @return 角色权限视图
     */
    @PostMapping("/getPermissionByRoleId")
    @PreAuthorize("@rbac.hasPerm('userInfo:roleInfo')")
    @Operation(summary = "查询角色已分配权限")
    public Result<RolePermVO> getPermissionByRoleId(@Valid @RequestBody RoleIdRequest request) {
        return Result.ok(sysAuthRoleService.getRolePermissionTree(request.getRoleId()));
    }
}
