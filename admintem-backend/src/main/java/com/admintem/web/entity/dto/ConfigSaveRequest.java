package com.admintem.web.entity.dto;

import com.admintem.web.entity.dto.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统参数新增/编辑请求。
 */
@Data
public class ConfigSaveRequest {

    @NotNull(message = "ID不能为空", groups = ValidationGroups.Update.class)
    @Schema(description = "主键（编辑时必填）")
    private Long id;

    @NotBlank(message = "参数键不能为空")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9._-]{1,63}$",
            message = "参数键须为字母开头的字母数字点/下划线/中划线，2~64 位")
    @Schema(description = "参数键", example = "system.upload.max-size")
    private String configKey;

    @NotBlank(message = "参数名称不能为空")
    @Size(max = 64, message = "参数名称最长64个字符")
    @Schema(description = "参数名称", example = "上传大小上限")
    private String configName;

    @Size(max = 2000, message = "参数值最长2000个字符")
    @Schema(description = "参数值")
    private String configValue;

    @Size(max = 32, message = "分组最长32个字符")
    @Schema(description = "分组", example = "upload")
    private String configGroup;

    @Size(max = 255, message = "备注最长255个字符")
    @Schema(description = "备注")
    private String remark;
}
