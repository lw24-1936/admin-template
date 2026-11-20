package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 批量操作的用户 ID 集合。 */
@Data
public class BatchIdsRequest {

    @NotEmpty(message = "请选择至少一条数据")
    @Size(max = 200, message = "一次最多操作 200 条")
    @Schema(description = "用户 ID 列表", example = "[2,3]")
    private List<Long> ids;
}
