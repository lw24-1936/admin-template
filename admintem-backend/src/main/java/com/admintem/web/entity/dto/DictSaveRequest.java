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

@Data
public class DictSaveRequest {

    @NotNull(message = "ID不能为空", groups = ValidationGroups.Update.class)
    private Long id;

    @NotBlank(message = "字典类型不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,31}$", message = "字典类型须为小写字母开头的字母数字下划线，2~32位")
    @Schema(description = "字典类型", example = "user_status")
    private String dictType;

    @NotBlank(message = "显示文案不能为空")
    @Size(max = 64, message = "显示文案最长64个字符")
    private String dictLabel;

    @NotBlank(message = "字典值不能为空")
    @Size(max = 64, message = "字典值最长64个字符")
    private String dictValue;

    @Min(value = 0, message = "排序号不能小于0")
    @Max(value = 9999, message = "排序号最大9999")
    private Integer sortNum = 0;

    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status = 0;

    @Size(max = 255, message = "备注最长255个字符")
    private String remark;
}
