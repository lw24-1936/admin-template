package com.admintem.web.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FileVO implements Serializable {

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
}
