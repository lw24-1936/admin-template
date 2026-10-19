package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.admintem.web.entity.dto.PageQuery;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RolePageQuery extends PageQuery {

    /** 角色名称模糊查询 */
    @Schema(description = "角色名称模糊查询", example = "运营专员")
    private String roleName;

    /** 角色 key 模糊查询 */
    @Schema(description = "角色 key 模糊查询", example = "operator")
    private String roleKey;
}
