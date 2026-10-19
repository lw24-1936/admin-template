package com.admintem.web.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点（树表渲染用）。
 *
 * <p>{@code userCount} 是该部门下的用户数，列表直接展示"这个部门有几个人"，
 * 避免运维逐个部门点进去查。</p>
 */
@Data
@Schema(description = "部门树节点")
public class DeptVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门ID")
    private Long id;

    @Schema(description = "上级部门ID（0 表示顶级）")
    private Long parentId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "同级排序号")
    private Integer orderNum;

    @Schema(description = "负责人")
    private String leader;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "状态：0 启用 / 1 禁用")
    private Integer status;

    @Schema(description = "该部门下的用户数")
    private Long userCount;

    @Schema(description = "子部门")
    private List<DeptVO> children = new ArrayList<>();
}
