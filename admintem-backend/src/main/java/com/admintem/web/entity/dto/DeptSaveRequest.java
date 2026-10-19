package com.admintem.web.entity.dto;

import com.admintem.web.entity.dto.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 部门新增/编辑入参。
 *
 * <p>新增与编辑共用一个 DTO，靠校验分组区分：编辑时 {@code id} 必填
 * （由 {@code ValidationGroups.Update} 约束），新增时忽略。</p>
 */
@Data
@Schema(description = "部门保存入参")
public class DeptSaveRequest {

    @NotNull(groups = ValidationGroups.Update.class, message = "部门ID不能为空")
    @Schema(description = "部门ID（编辑时必填）", example = "1")
    private Long id;

    @NotNull(message = "上级部门不能为空（顶级填 0）")
    @Schema(description = "上级部门ID，0 表示顶级", example = "0")
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 64, message = "部门名称长度不能超过 64")
    @Schema(description = "部门名称", example = "技术部")
    private String deptName;

    @Min(value = 1, message = "排序号最小为 1")
    @Max(value = 9999, message = "排序号最大为 9999")
    @Schema(description = "同级排序号（升序）", example = "1")
    private Integer orderNum = 1;

    @Size(max = 32, message = "负责人长度不能超过 32")
    @Schema(description = "负责人", example = "张三")
    private String leader;

    @Pattern(regexp = "^$|^[0-9-+() ]{6,20}$", message = "联系电话格式不正确")
    @Schema(description = "联系电话", example = "010-00000002")
    private String phone;

    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    @Schema(description = "状态：0 启用 / 1 禁用", example = "0")
    private Integer status = 0;
}
