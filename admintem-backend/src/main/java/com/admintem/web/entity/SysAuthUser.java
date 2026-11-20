package com.admintem.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息表实体（sys_auth_user）。
 *
 * <p>对应数据库表字段与旧项目完全一致；{@code password} 字段仅允许写入、
 * 禁止序列化输出，避免泄露密码散列。</p>
 */
@Data
@Schema(description = "用户实体")
public class SysAuthUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Schema(description = "用户ID")
    private Long id;

    /** 用户名称/账号（登录名） */
    @Schema(description = "账号")
    private String userName;

    /** 昵称 */
    @Schema(description = "昵称")
    private String nickName;

    /** 邮箱 */
    @Schema(description = "邮箱")
    private String email;

    /** 手机号 */
    @Schema(description = "手机号")
    private String phone;

    /** 密码（BCrypt 散列；只写不读） */
    @JsonIgnore
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "密码（仅写入使用，BCrypt 加密存储）")
    private String password;

    /** 性别（0：女，1：男） */
    @Schema(description = "性别（0女 1男）")
    private Integer sex;

    /** 头像 URL */
    @Schema(description = "头像")
    private String avatar;

    /** 所属部门ID（V1.0.9；可为空表示未分配） */
    private Long deptId;

    /** 状态（0启用 1禁用） */
    @Schema(description = "状态（0启用 1禁用）")
    private Integer status;

    /** 个人介绍 */
    @Schema(description = "个人介绍")
    private String introduce;

    /** 特殊扩展字段 */
    @Schema(description = "扩展字段")
    private String extJson;

    /** TOTP Base32 secret（不返回给普通查询） */
    @JsonIgnore
    @Schema(description = "TOTP secret", hidden = true)
    private String mfaSecret;

    /** MFA 是否开启：0 关闭 / 1 开启 */
    @Schema(description = "MFA 是否开启：0 关闭 / 1 开启")
    private Integer mfaEnabled;

    /** 创建人 */
    @Schema(description = "创建人")
    private String createdBy;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /** 更新人 */
    @Schema(description = "更新人")
    private String updateBy;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0未删除 1已删除） */
    @JsonIgnore
    @Schema(description = "逻辑删除标记", hidden = true)
    private Integer isDeleted;
}
