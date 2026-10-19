package com.admintem.web.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 当前登录用户完整会话视图（登录/获取用户信息共用）。
 */
@Data
public class UserSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 账号 */
    private String userName;

    /** 昵称 */
    private String nickName;

    /** 头像 */
    private String avatar;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 性别 */
    private Integer sex;

    /** 个人介绍 */
    private String introduce;

    /** 账号状态（0启用 1禁用） */
    private Integer status;

    /** 拥有的角色列表 */
    private List<RoleVO> roles = new ArrayList<>();

    /** 可访问菜单树（type=0） */
    private List<MenuVO> menus = new ArrayList<>();

    /** 全部权限标识（菜单 + 按钮的 permission_key），供 v-perm 指令判断按钮显隐 */
    private Set<String> permissions = new HashSet<>();
}
