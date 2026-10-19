package com.admintem.web.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户列表行视图对象。
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 账号 */
    private String userName;

    /** 昵称 */
    private String nickName;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 性别（0女 1男） */
    private Integer sex;

    /** 头像 */
    private String avatar;

    /** 状态（0启用 1禁用） */
    private Integer status;

    /** 个人介绍 */
    private String introduce;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 拥有的角色ID列表 */
    private Long deptId;

    /** 部门名称（列表展示用；未分配为空） */
    private String deptName;

    private List<Long> roleIds = new ArrayList<>();

    /** 拥有的角色名称列表（便于列表展示） */
    private List<String> roleNames = new ArrayList<>();
}
