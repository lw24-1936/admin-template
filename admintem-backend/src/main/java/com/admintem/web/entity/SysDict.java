package com.admintem.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据字典实体（sys_dict）。
 */
@Data
@Schema(description = "数据字典")
public class SysDict implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer sortNum;
    private Integer status;
    private String remark;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updateBy;
    private LocalDateTime updateTime;

    @JsonIgnore
    @Schema(hidden = true)
    private Integer isDeleted;
}
