package com.admintem.web.entity.vo;

import lombok.Data;

/** 图形验证码挑战：PNG 图片；debugCode 仅 dev 返回，供自动化测试。 */
@Data
public class CaptchaVO {
    private String token;
    private String image;
    private int width;
    private int height;
    private int length;
    private Integer debugCode;
    private String debugText;
}
