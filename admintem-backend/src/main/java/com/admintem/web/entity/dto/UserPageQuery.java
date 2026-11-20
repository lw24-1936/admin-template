package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;

import com.admintem.web.entity.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

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

    /**
     * 数据范围由服务端根据角色写入，禁止客户端伪造。
     * 使用 @JsonIgnore 避免请求体覆盖。
     */
    @JsonIgnore
    private String dataScope;

    /** 数据范围对应的部门 ID（服务端写入） */
    @JsonIgnore
    private List<Long> dataScopeDeptIds;

    /** SELF 范围对应的当前用户 ID（服务端写入） */
    @JsonIgnore
    private Long dataScopeUserId;
}
