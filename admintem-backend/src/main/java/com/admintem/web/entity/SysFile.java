package com.admintem.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件中心元数据（字节由 StorageStrategy 保存）。
 */
@Data
@Schema(description = "文件中心")
public class SysFile implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String originalName;
    private String url;
    private Long fileSize;
    private String contentType;
    private String storageType;
    private String remark;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updateBy;
    private LocalDateTime updateTime;

    @JsonIgnore
    @Schema(hidden = true)
    private Integer isDeleted;
}
