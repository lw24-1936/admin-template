package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 自助注册请求体。
 */
@Data
public class RegisterRequest {

    /** 登录账号（字母/数字/下划线，4~32 位） */
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "账号须为4~32位字母、数字或下划线")
    @Schema(description = "登录账号（字母/数字/下划线，4~32 位）", example = "zhangsan")
    private String userName;

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称最长32个字符")
    @Schema(description = "昵称", example = "张三")
    private String nickName;

    /** 密码（6~32 位） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为6~32位")
    @Schema(description = "密码（6~32 位）", example = "12345678")
    private String password;

    /** 邮箱（可选） */
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱（可选）", example = "zhangsan@example.com")
    private String email;

    /** 手机号（可选） */
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    @Schema(description = "手机号（可选）", example = "13800138000")
    private String phone;

    /** 性别（0女 1男，可选） */
    @Schema(description = "性别（0女 1男，可选）", example = "1")
    private Integer sex;
}
