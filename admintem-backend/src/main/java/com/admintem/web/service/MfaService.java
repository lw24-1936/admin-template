package com.admintem.web.service;

import com.admintem.web.entity.vo.MfaSetupVO;

/** 用户 MFA（TOTP）管理。 */
public interface MfaService {

    boolean isEnabled(Long userId);

    MfaSetupVO setup(Long userId);

    void enable(Long userId, String code);

    void disable(Long userId, String password, String code);
}
