package com.admintem.web.entity.vo;

import lombok.Data;

/** 图形验证码挑战；图片为 data URI，debugOffset 仅 dev 返回。 */
@Data
public class CaptchaVO {
    private String token;
    private String background;
    private String piece;
    private int width;
    private int height;
    private int pieceSize;
    private int pieceY;
    private Integer debugOffset;
}
