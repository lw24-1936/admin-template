package com.admintem.web.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 字典类型查询请求。 */
@Data
public class DictTypeRequest {

    @NotBlank(message = "字典类型不能为空")
    private String dictType;
}
