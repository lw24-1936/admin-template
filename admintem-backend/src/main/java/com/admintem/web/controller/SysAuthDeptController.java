package com.admintem.web.controller;

import com.admintem.web.aspect.AuditLog;
import com.admintem.web.aspect.AuditTargetType;
import com.admintem.web.common.Result;
import com.admintem.web.entity.dto.DeptSaveRequest;
import com.admintem.web.entity.dto.IdRequest;
import com.admintem.web.entity.dto.validation.ValidationGroups;
import com.admintem.web.entity.vo.DeptVO;
import com.admintem.web.security.SecurityUtils;
import com.admintem.web.service.SysAuthDeptService;
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

import java.util.List;

/**
 * 部门管理控制器（sys_dept，V1.0.9）。
 *
 * <p>按钮级权限标识：新增 sysAuthDept:addDept、编辑 sysAuthDept:editDept、
 * 删除 sysAuthDept:deleteById；列表依赖「部门管理」菜单权限 dept:dept。</p>
 *
 * <p>删除有两条业务保护（都在服务层）：存在子部门、或部门下仍有用户时拒绝，
 * 避免出现"用户挂在不存在的部门上"这类脏数据。</p>
 */
@Slf4j
@RestController
@RequestMapping("/sysAuthDept")
@RequiredArgsConstructor
@Tag(name = "08-部门管理", description = "树形组织：部门 CRUD")
public class SysAuthDeptController {

    private final SysAuthDeptService sysAuthDeptService;

    /**
     * 部门树（含各部门用户数）。
     *
     * @return 顶级部门列表
     */
    @PostMapping("/treeList")
    @PreAuthorize("@rbac.hasPerm('dept:dept')")
    @Operation(summary = "查询部门树")
    public Result<List<DeptVO>> treeList() {
        return Result.ok(sysAuthDeptService.treeList());
    }

    /**
     * 新增部门。
     */
    @PostMapping("/addDept")
    @AuditLog(action = "DEPT_ADD", name = "新增部门", target = AuditTargetType.ROLE)
    @PreAuthorize("@rbac.hasPerm('sysAuthDept:addDept')")
    @Operation(summary = "新增部门")
    public Result<Long> addDept(@Valid @RequestBody DeptSaveRequest request) {
        return Result.ok(sysAuthDeptService.createDept(request, SecurityUtils.currentUserName()));
    }

    /**
     * 编辑部门（含移动上级）。
     */
    @PostMapping("/editDept")
    @AuditLog(action = "DEPT_EDIT", name = "编辑部门", target = AuditTargetType.ROLE)
    @PreAuthorize("@rbac.hasPerm('sysAuthDept:editDept')")
    @Operation(summary = "编辑部门")
    public Result<Void> editDept(@Validated(ValidationGroups.Update.class) @RequestBody DeptSaveRequest request) {
        sysAuthDeptService.updateDept(request, SecurityUtils.currentUserName());
        return Result.ok();
    }

    /**
     * 删除部门。
     */
    @PostMapping("/deleteById")
    @AuditLog(action = "DEPT_DELETE", name = "删除部门", target = AuditTargetType.ROLE)
    @PreAuthorize("@rbac.hasPerm('sysAuthDept:deleteById')")
    @Operation(summary = "删除部门")
    public Result<Void> deleteById(@Valid @RequestBody IdRequest request) {
        sysAuthDeptService.deleteDept(request.getId(), SecurityUtils.currentUserName());
        return Result.ok();
    }
}
