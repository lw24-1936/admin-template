package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.admintem.web.entity.dto.validation.ValidationGroups;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户新增/编辑请求体（管理端）。
 *
 * <p>新增时 password 必填；编辑时 password 为空表示不修改密码。
 * 可同时携带 roleIds 批量分配角色。</p>
 */
@Data
public class UserSaveRequest {

    /** 用户ID（编辑时必填；分组校验：仅 Update 分组要求非空） */
    @NotNull(message = "ID不能为空", groups = ValidationGroups.Update.class)
    @Schema(description = "用户ID（编辑时必填；分组校验：仅 Update 分组要求非空）", example = "3")
    private Long id;

    /** 账号（新增时必填，编辑时不可修改） */
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "账号须为4~32位字母、数字或下划线")
    @Schema(description = "账号（新增时必填，编辑时不可修改）", example = "zhangsan")
    private String userName;

    /** 昵称 */
    @Size(max = 32, message = "昵称最长32个字符")
    @Schema(description = "昵称", example = "张三")
    private String nickName;

    /** 密码（新增必填；编辑时留空表示不改） */
    @Size(min = 6, max = 32, message = "密码长度须为6~32位")
    @Schema(description = "密码（新增必填；编辑时留空表示不改）", example = "12345678")
    private String password;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    /** 手机号 */
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    /** 性别（0女 1男） */
    @Schema(description = "性别（0女 1男）", example = "1")
    private Integer sex;

    /** 头像 URL */
    @Schema(description = "头像 URL", example = "https://example.com/avatar.png")
    private String avatar;

    /** 状态（0启用 1禁用） */
    @Schema(description = "状态（0启用 1禁用）", example = "0")
    private Integer status;

    /** 所属部门ID（可空；为空表示不调整/不分配） */
    private Long deptId;

    /** 个人介绍 */
    @Size(max = 255, message = "个人介绍最长255个字符")
    @Schema(description = "个人介绍", example = "示例简介")
    private String introduce;

    /** 分配的角色 ID 列表（可选） */
    @Schema(description = "分配的角色 ID 列表（可选）", example = "[2,3]")
    private List<Long> roleIds;
}
