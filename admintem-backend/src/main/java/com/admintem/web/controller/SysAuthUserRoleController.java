package com.admintem.web.controller;

import com.admintem.web.common.Result;
import com.admintem.web.entity.dto.UserIdRequest;
import com.admintem.web.service.SysAuthUserService;
import com.admintem.web.entity.vo.UserRoleVO;
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
 * 用户-角色关系查询控制器。
 *
 * <p>查询用户已分配的角色（用户管理页的"角色分配"弹窗需要回显）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/sysAuthUserRole")
@RequiredArgsConstructor
@Tag(name = "02B-用户角色查询", description = "查询用户已分配角色")
public class SysAuthUserRoleController {

    private final SysAuthUserService sysAuthUserService;

    /**
     * 根据用户 ID 查询其角色（需要"用户管理"菜单权限）。
     *
     * @param request 用户ID
     * @return 用户角色视图
     */
    @PostMapping("/getUserRoleByUserId")
    @PreAuthorize("@rbac.hasPerm('userInfo:userInfo')")
    @Operation(summary = "查询用户已分配角色")
    public Result<UserRoleVO> getUserRoleByUserId(@Valid @RequestBody UserIdRequest request) {
        return Result.ok(sysAuthUserService.getUserRoles(request.getUserId()));
    }
}
