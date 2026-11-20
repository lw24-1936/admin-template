package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 批量启用/禁用请求。 */
@Data
public class BatchStatusRequest {

    @NotEmpty(message = "请选择至少一条数据")
    @Size(max = 200, message = "一次最多操作 200 条")
    @Schema(description = "用户 ID 列表", example = "[2,3]")
    private List<Long> ids;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    @Schema(description = "0 启用 / 1 禁用", example = "0")
    private Integer status;
}
