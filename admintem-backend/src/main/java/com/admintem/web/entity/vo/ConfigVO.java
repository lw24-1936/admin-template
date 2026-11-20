package com.admintem.web.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统参数视图对象。
 */
@Data
public class ConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private String configGroup;
    private String remark;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
