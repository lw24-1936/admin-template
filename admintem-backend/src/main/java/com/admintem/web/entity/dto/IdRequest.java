package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 按主键操作请求体（删除等）。
 */
@Data
public class IdRequest {

    /** 主键 ID */
    @NotNull(message = "ID不能为空")
    @Schema(description = "主键 ID", example = "3")
    private Long id;
}
