package com.admintem.web.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.admintem.web.common.ResultCode;
import com.admintem.web.entity.SysConfig;
import com.admintem.web.entity.dto.ConfigPageQuery;
import com.admintem.web.entity.dto.ConfigSaveRequest;
import com.admintem.web.entity.vo.ConfigVO;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysAuthConfigMapper;
import com.admintem.web.service.SysAuthConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysAuthConfigServiceImpl implements SysAuthConfigService {

    private final SysAuthConfigMapper configMapper;

    @Override
    public PageResult<ConfigVO> pageQuery(ConfigPageQuery query) {
        long pageNum = query.getPageNum() == null ? 1L : query.getPageNum();
        long pageSize = query.getPageSize() == null ? 10L : query.getPageSize();
        IPage<SysConfig> page = configMapper.selectConfigPage(new Page<>(pageNum, pageSize), query);
        List<ConfigVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<ConfigVO> listAll() {
        return configMapper.selectAllOrdered().stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long create(ConfigSaveRequest request, String operator) {
        if (configMapper.countByKey(request.getConfigKey(), null) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "参数键已存在: " + request.getConfigKey());
        }
        SysConfig config = new SysConfig();
        config.setConfigKey(request.getConfigKey());
        config.setConfigValue(request.getConfigValue());
        config.setConfigName(request.getConfigName());
        config.setConfigGroup(StringUtils.hasText(request.getConfigGroup()) ? request.getConfigGroup() : "default");
        config.setRemark(request.getRemark());
        config.setCreatedBy(operator);
        config.setCreatedTime(LocalDateTime.now());
        try {
            configMapper.insert(config);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "参数键已存在: " + request.getConfigKey());
        }
        return config.getId();
    }

    @Override
    @Transactional
    public void update(ConfigSaveRequest request, String operator) {
        SysConfig config = configMapper.selectById(request.getId());
        if (config == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "参数不存在");
        }
        if (configMapper.countByKey(request.getConfigKey(), request.getId()) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "参数键已存在: " + request.getConfigKey());
        }
        config.setConfigKey(request.getConfigKey());
        config.setConfigValue(request.getConfigValue());
        config.setConfigName(request.getConfigName());
        config.setConfigGroup(StringUtils.hasText(request.getConfigGroup()) ? request.getConfigGroup() : "default");
        config.setRemark(request.getRemark());
        config.setUpdateBy(operator);
        config.setUpdateTime(LocalDateTime.now());
        try {
            configMapper.updateSelective(config);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "参数键已存在: " + request.getConfigKey());
        }
    }

    @Override
    @Transactional
    public void delete(Long id, String operator) {
        if (configMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "参数不存在");
        }
        configMapper.logicDelete(id, operator, LocalDateTime.now());
    }

    @Override
    public String getValue(String key) {
        SysConfig config = configMapper.selectByKey(key);
        return config == null ? null : config.getConfigValue();
    }

    private ConfigVO toVO(SysConfig config) {
        ConfigVO vo = new ConfigVO();
        vo.setId(config.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setConfigValue(config.getConfigValue());
        vo.setConfigName(config.getConfigName());
        vo.setConfigGroup(config.getConfigGroup());
        vo.setRemark(config.getRemark());
        vo.setCreatedBy(config.getCreatedBy());
        vo.setCreatedTime(config.getCreatedTime());
        vo.setUpdateBy(config.getUpdateBy());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }
}
