package com.admintem.web.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门实体（sys_dept，V1.0.9）。
 *
 * <p>树形结构用 {@code parentId} 表示，0 为顶级。与权限树一样，
 * 这里**不使用**路径枚举或闭包表——部门层级通常不超过 5 层，
 * 全量查询后在内存里建树最简单，也避免维护额外的一致性。</p>
 */
@Data
@Schema(description = "部门")
public class SysDept implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门ID")
    private Long id;

    @Schema(description = "上级部门ID（0 表示顶级）")
    private Long parentId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "同级排序号（升序）")
    private Integer orderNum;

    @Schema(description = "负责人")
    private String leader;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "状态：0 启用 / 1 禁用")
    private Integer status;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "是否删除：0 否 / 1 是")
    private Integer isDeleted;

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 0;

    /** 状态：禁用 */
    public static final int STATUS_DISABLED = 1;

    /** 顶级部门标识 */
    public static final long ROOT_PARENT_ID = 0L;
}
