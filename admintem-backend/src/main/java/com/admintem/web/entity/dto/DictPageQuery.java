package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DictPageQuery extends PageQuery {

    @Schema(description = "字典类型模糊查询")
    private String dictType;

    @Schema(description = "显示文案模糊查询")
    private String dictLabel;

    @Schema(description = "状态 0启用 1禁用")
    private Integer status;
}
