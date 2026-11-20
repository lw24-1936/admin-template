package com.admintem.web.controller;

import com.admintem.web.aspect.AuditLog;
import com.admintem.web.aspect.AuditTargetType;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.common.Result;
import com.admintem.web.entity.dto.AssignRoleRequest;
import com.admintem.web.entity.dto.BatchIdsRequest;
import com.admintem.web.entity.dto.BatchStatusRequest;
import com.admintem.web.entity.dto.IdRequest;
import com.admintem.web.entity.dto.ResetPasswordRequest;
import com.admintem.web.entity.dto.UserPageQuery;
import com.admintem.web.entity.dto.UserSaveRequest;
import com.admintem.web.entity.dto.validation.ValidationGroups;
import com.admintem.web.security.SecurityUtils;
import com.admintem.web.service.SysAuthUserService;
import com.admintem.web.service.DataScopeService;
import com.admintem.web.entity.vo.UserImportResultVO;
import com.admintem.web.entity.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用户管理控制器（sys_auth_user）。
 *
 * <p>按钮级权限标识说明（需与 sys_auth_permission 表中 type=1 记录一致）：
 * <ul>
 *   <li>查看列表：userInfo:userInfo（菜单权限，能进入页面即可查看）；</li>
 *   <li>新增用户：sysAuthUser:addSysAuthUser；</li>
 *   <li>编辑用户：sysAuthUser:editSysAuthUser；</li>
 *   <li>删除用户：sysAuthUser:deleteById；</li>
 *   <li>重置密码：sysAuthUser:resetPassword；</li>
 *   <li>分配角色：sysAuthUser:assignRole。</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/sysAuthUser")
@RequiredArgsConstructor
@Tag(name = "02-用户管理", description = "用户 CRUD、重置密码与角色分配")
public class SysAuthUserController {

    private final SysAuthUserService sysAuthUserService;
    private final DataScopeService dataScopeService;

    /**
     * 分页查询用户列表（需要"用户管理"菜单权限）。
     *
     * <p>加 @Valid 才能真正启用 PageQuery 上的 pageNum/pageSize 边界约束，
     * 否则 @Min/@Max 只是无效注解。</p>
     *
     * @param query 分页查询条件（pageNum ≥ 1，pageSize 1~500）
     * @return 分页用户
     */
    @PostMapping("/paginQuery")
    @PreAuthorize("@rbac.hasPerm('userInfo:userInfo')")
    @Operation(summary = "分页查询用户")
    public Result<PageResult<UserVO>> paginQuery(@Valid @RequestBody UserPageQuery query) {
        // 行级数据权限由服务端注入，忽略客户端伪造的 dataScope 字段
        dataScopeService.apply(query);
        return Result.ok(sysAuthUserService.pageQuery(query));
    }

    @PostMapping("/export")
    @AuditLog(action = "USER_EXPORT", name = "导出用户", target = AuditTargetType.USER)
    @PreAuthorize("@rbac.hasPerm('userInfo:userInfo')")
    @Operation(summary = "导出用户 CSV")
    public void export(@Valid @RequestBody UserPageQuery query, HttpServletResponse response) throws IOException {
        dataScopeService.apply(query);
        List<UserVO> users = sysAuthUserService.listForExport(query);
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=users.csv");
        PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        writer.write('\uFEFF');
        writer.println("id,userName,nickName,email,phone,deptId,status,password,createdTime");
        for (UserVO user : users) {
            writer.println(String.join(",",
                    csv(user.getId()),
                    csv(user.getUserName()),
                    csv(user.getNickName()),
                    csv(user.getEmail()),
                    csv(user.getPhone()),
                    csv(user.getDeptId()),
                    csv(user.getStatus()),
                    csv(""),
                    csv(user.getCreatedTime())));
        }
        writer.flush();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditLog(action = "USER_IMPORT", name = "导入用户", target = AuditTargetType.USER)
    @PreAuthorize("@rbac.hasPerm('sysAuthUser:addSysAuthUser')")
    @Operation(summary = "导入用户 CSV")
    public Result<UserImportResultVO> importCsv(@RequestParam("file") MultipartFile file) {
        return Result.ok(sysAuthUserService.importCsv(file, SecurityUtils.currentUserName()));
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        // 防止 CSV 公式注入（Excel/Numbers 打开时不执行）
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    /**
     * 新增用户（需要按钮权限 sysAuthUser:addSysAuthUser）。
     *
     * @param request 请求体
     * @return 新用户ID
     */
    @PostMapping("/addSysAuthUser")
    @AuditLog(action = "USER_ADD", name = "新增用户", target = AuditTargetType.USER)
    @PreAuthorize("@rbac.hasPerm('sysAuthUser:addSysAuthUser')")
    @Operation(summary = "新增用户")
    public Result<Long> addSysAuthUser(@Validated(ValidationGroups.Create.class) @RequestBody UserSaveRequest request) {
        return Result.ok(sysAuthUserService.createUser(request, SecurityUtils.currentUserName()));
    }

    /**
     * 编辑用户（需要按钮权限 sysAuthUser:editSysAuthUser）。
     *
     * @param request 请求体
     * @return 成功提示
     */
    @PostMapping("/editSysAuthUser")
    @AuditLog(action = "USER_EDIT", name = "编辑用户", target = AuditTargetType.USER)
    @PreAuthorize("@rbac.hasPerm('sysAuthUser:editSysAuthUser')")
    @Operation(summary = "编辑用户")
    public Result<Void> editSysAuthUser(@Validated(ValidationGroups.Update.class) @RequestBody UserSaveRequest request) {
        sysAuthUserService.updateUser(request, SecurityUtils.currentUserName());
        return Result.ok();
    }

    /**
     * 删除用户（逻辑删除；需要按钮权限 sysAuthUser:deleteById）。
     *
     * @param request ID 请求体
     * @return 成功提示
     */
    @PostMapping("/deleteById")
    @AuditLog(action = "USER_DELETE", name = "删除用户", target = AuditTargetType.USER)
    @PreAuthorize("@rbac.hasPerm('sysAuthUser:deleteById')")
    @Operation(summary = "删除用户")
    public Result<Void> deleteById(@Valid @RequestBody IdRequest request) {
        sysAuthUserService.deleteUser(request.getId(), SecurityUtils.currentUserName());
        return Result.ok();
    }

    @PostMapping("/batchStatus")
    @AuditLog(action = "USER_BATCH_STATUS", name = "批量更新用户状态", target = AuditTargetType.USER)
    @PreAuthorize("@rbac.hasPerm('sysAuthUser:editSysAuthUser')")
    @Operation(summary = "批量启用/禁用用户")
    public Result<Void> batchStatus(@Valid @RequestBody BatchStatusRequest request) {
        sysAuthUserService.batchUpdateStatus(
                request.getIds(), request.getStatus(), SecurityUtils.currentUserName());
        return Result.ok();
    }

    @PostMapping("/batchDelete")
    @AuditLog(action = "USER_BATCH_DELETE", name = "批量删除用户", target = AuditTargetType.USER)
    @PreAuthorize("@rbac.hasPerm('sysAuthUser:deleteById')")
    @Operation(summary = "批量删除用户")
    public Result<Void> batchDelete(@Valid @RequestBody BatchIdsRequest request) {
        sysAuthUserService.batchDelete(request.getIds(), SecurityUtils.currentUserName());
        return Result.ok();
    }

    /**
     * 重置用户密码（需要按钮权限 sysAuthUser:resetPassword）。
     *
     * @param request 请求体
     * @return 成功提示
     */
    @PostMapping("/resetPassword")
    @AuditLog(action = "USER_RESET_PWD", name = "重置用户密码", target = AuditTargetType.USER)
    @PreAuthorize("@rbac.hasPerm('sysAuthUser:resetPassword')")
    @Operation(summary = "重置用户密码")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        sysAuthUserService.resetPassword(request, SecurityUtils.currentUserName());
        return Result.ok("密码已重置，用户下次登录请使用新密码", null);
    }

    /**
     * 分配用户角色（需要按钮权限 sysAuthUser:assignRole）。
     *
     * @param request 请求体
     * @return 成功提示
     */
    @PostMapping("/assignRole")
    @AuditLog(action = "USER_ASSIGN_ROLE", name = "分配用户角色", target = AuditTargetType.USER)
    @PreAuthorize("@rbac.hasPerm('sysAuthUser:assignRole')")
    @Operation(summary = "分配用户角色")
    public Result<Void> assignRole(@Valid @RequestBody AssignRoleRequest request) {
        sysAuthUserService.assignRoles(request, SecurityUtils.currentUserName());
        return Result.ok();
    }
}
