package com.admintem.web.entity.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 角色视图对象。
 */
@Data
public class RoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色ID */
    private Long id;

    /** 角色名称 */
    private String roleName;

    /** 角色唯一标识 */
    private String roleKey;

    /** 数据范围：ALL/DEPT/DEPT_AND_CHILD/SELF */
    private String dataScope;
}
