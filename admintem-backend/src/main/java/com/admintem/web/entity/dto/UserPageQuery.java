package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.admintem.web.entity.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageQuery extends PageQuery {

    /** 账号模糊查询 */
    @Schema(description = "账号模糊查询", example = "zhangsan")
    private String userName;

    /** 昵称模糊查询 */
    @Schema(description = "昵称模糊查询", example = "张三")
    private String nickName;

    /** 手机号模糊查询 */
    @Schema(description = "手机号模糊查询", example = "13800138000")
    private String phone;

    /** 状态（0启用 1禁用） */
    @Schema(description = "状态（0启用 1禁用）", example = "0")
    private Integer status;
}
