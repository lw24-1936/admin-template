package com.admintem.web.entity.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询统一返回结构。
 *
 * <p>将 MyBatis-Plus 的 {@link IPage} 转换为与前端约定的扁平结构：</p>
 * <ul>
 *   <li>{@code records}：当前页数据；</li>
 *   <li>{@code total}：总记录数；</li>
 *   <li>{@code pageNum}/{@code pageSize}：回显当前页码与每页大小。</li>
 * </ul>
 *
 * @param <T> 行数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页返回结构")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    @Schema(description = "当前页数据列表")
    private List<T> records;

    /** 总记录数 */
    @Schema(description = "总记录数")
    private long total;

    /** 当前页码 */
    @Schema(description = "当前页码")
    private long pageNum;

    /** 每页大小 */
    @Schema(description = "每页大小")
    private long pageSize;

    /** 由 MyBatis-Plus 分页对象转换 */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
