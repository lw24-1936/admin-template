package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人资料修改请求体（用户修改自己的昵称/联系方式等）。
 */
@Data
public class UpdateProfileRequest {

    /** 昵称 */
    @Size(max = 32, message = "昵称最长32个字符")
    @Schema(description = "昵称", example = "张三")
    private String nickName;

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

    /** 个人介绍 */
    @Size(max = 255, message = "个人介绍最长255个字符")
    @Schema(description = "个人介绍", example = "示例简介")
    private String introduce;
}
