package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FilePageQuery extends PageQuery {

    @Schema(description = "原始文件名模糊查询")
    private String originalName;
}
