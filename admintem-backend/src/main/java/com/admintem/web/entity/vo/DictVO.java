package com.admintem.web.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DictVO implements Serializable {

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
}
