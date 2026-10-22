package com.admintem.web.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 验证码通过后的一次性票据。 */
@Data
@AllArgsConstructor
public class CaptchaTicketVO {
    private String ticket;
}
