package com.admintem.web.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 在线会话（一条 = 一个账号当前的有效令牌）。
 *
 * <p>数据来源是 Redis 里的令牌白名单：key 为 {@code admintem:auth:jwt:<账号>}，
 * 值是当前有效令牌。因此"在线"的准确含义是
 * <b>持有未过期令牌且未被吊销</b>——而不是"浏览器标签页开着"。</p>
 */
@Data
@Schema(description = "在线会话")
public class OnlineSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "登录账号")
    private String userName;

    @Schema(description = "昵称（查不到用户时为空）")
    private String nickName;

    @Schema(description = "账号状态：0 启用 / 1 禁用")
    private Integer status;

    @Schema(description = "所属部门名称（V1.0.9 起；未分配为空）")
    private String deptName;

    @Schema(description = "角色名（逗号分隔）")
    private String roleNames;

    @Schema(description = "令牌剩余有效秒数")
    private Long expireSeconds;

    @Schema(description = "令牌摘要（前 8 位，便于区分同一账号的多条会话，不泄露完整令牌）")
    private String tokenDigest;

    @Schema(description = "是否为当前请求所在的会话")
    private Boolean current;

    @Schema(description = "是否处于登录锁定状态（锁定中的账号即使有令牌也会被拒绝）")
    private Boolean locked;
}
