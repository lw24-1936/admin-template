package com.admintem.web.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户-角色视图：用户已分配角色 ID 列表。
 */
@Data
public class UserRoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 已分配角色 ID 列表 */
    private List<Long> roleIds = new ArrayList<>();
}
