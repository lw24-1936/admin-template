package com.admintem.web.service.impl;

import com.admintem.web.common.ResultCode;
import com.admintem.web.entity.SysAuthUser;
import com.admintem.web.entity.vo.MfaSetupVO;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysAuthUserMapper;
import com.admintem.web.security.TotpService;
import com.admintem.web.service.MfaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** MFA 管理实现：setup 生成 secret，enable/disable 都要 TOTP 校验。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {

    private static final String ISSUER = "admintem";

    private final SysAuthUserMapper userMapper;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean isEnabled(Long userId) {
        SysAuthUser user = userMapper.selectById(userId);
        return user != null && Integer.valueOf(1).equals(user.getMfaEnabled());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfaSetupVO setup(Long userId) {
        SysAuthUser user = requireUser(userId);
        if (Integer.valueOf(1).equals(user.getMfaEnabled())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "MFA 已开启，请先关闭再重新绑定");
        }
        String secret = totpService.generateSecret();
        userMapper.updateMfa(userId, secret, 0);
        log.info("MFA 绑定已初始化: userId={}", userId);
        return new MfaSetupVO(secret, totpService.buildOtpAuthUri(secret, user.getUserName(), ISSUER));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long userId, String code) {
        SysAuthUser user = requireUser(userId);
        if (Integer.valueOf(1).equals(user.getMfaEnabled())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "MFA 已开启");
        }
        if (user.getMfaSecret() == null || !totpService.verify(user.getMfaSecret(), code)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误或已过期");
        }
        userMapper.updateMfa(userId, user.getMfaSecret(), 1);
        log.info("MFA 已开启: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long userId, String password, String code) {
        SysAuthUser user = requireUser(userId);
        if (!Integer.valueOf(1).equals(user.getMfaEnabled())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "MFA 未开启");
        }
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "密码错误");
        }
        if (!totpService.verify(user.getMfaSecret(), code)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误或已过期");
        }
        userMapper.updateMfa(userId, null, 0);
        log.info("MFA 已关闭: userId={}", userId);
    }

    private SysAuthUser requireUser(Long userId) {
        SysAuthUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在");
        }
        return user;
    }
}
