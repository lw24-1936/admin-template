package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigPageQuery extends PageQuery {

    @Schema(description = "参数键模糊查询")
    private String configKey;

    @Schema(description = "参数名称模糊查询")
    private String configName;

    @Schema(description = "分组")
    private String configGroup;
}
