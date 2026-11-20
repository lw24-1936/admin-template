package com.admintem.web.service;

import com.admintem.web.entity.dto.SsoLoginRequest;
import com.admintem.web.entity.vo.LoginVO;

/** 外部网关 SSO 断言登录。 */
public interface SsoService {

    LoginVO login(SsoLoginRequest request);
}
