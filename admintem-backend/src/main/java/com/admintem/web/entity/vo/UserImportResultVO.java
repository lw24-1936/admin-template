package com.admintem.web.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 用户 CSV 导入结果。 */
@Data
public class UserImportResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int total;
    private int success;
    private int failed;
    private List<String> errors = new ArrayList<>();
}
