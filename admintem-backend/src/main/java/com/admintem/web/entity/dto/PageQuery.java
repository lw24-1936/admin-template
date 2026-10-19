package com.admintem.web.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分页查询基础入参。
 *
 * <p>所有列表查询接口的 DTO 均可继承本类，统一携带分页参数；
 * 默认第 1 页、每页 10 条。</p>
 *
 * <p>排序参数 {@code sortField/sortOrder} 为**字段名 + 方向**的形式，
 * 服务端按各资源的白名单映射为真实列名后再拼入 SQL（见
 * {@link com.admintem.web.util.query.SortWhitelist}），因此不会发生 SQL 注入：
 * 未登记的字段一律忽略并回落默认排序。</p>
 */
@Data
@Schema(description = "分页查询基础参数")
public class PageQuery {

    /** 页码（从 1 开始） */
    @Min(value = 1, message = "pageNum 最小为 1")
    @Max(value = 1000, message = "pageNum 最大为 1000（请用筛选条件缩小范围，避免深分页全表扫描）")
    @Schema(description = "页码，从 1 开始；上限 1000（防深分页）", example = "1")
    private Integer pageNum = 1;

    /** 每页条数 */
    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 500, message = "pageSize 最大为 500")
    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    /** 排序字段（逻辑字段名，如 userName；需在各资源白名单内，否则忽略） */
    @Size(max = 64, message = "sortField 长度不能超过 64")
    @Pattern(regexp = "^[A-Za-z0-9_]*$", message = "sortField 仅允许字母、数字与下划线")
    @Schema(description = "排序字段（逻辑名，需在白名单内）", example = "userName")
    private String sortField;

    /** 排序方向（asc / desc，缺省 asc） */
    @Size(max = 8, message = "sortOrder 长度不能超过 8")
    @Schema(description = "排序方向：asc 或 desc", example = "asc")
    private String sortOrder;
}
