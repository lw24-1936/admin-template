package com.admintem.web.service;

import com.admintem.web.entity.dto.ConfigPageQuery;
import com.admintem.web.entity.dto.ConfigSaveRequest;
import com.admintem.web.entity.vo.ConfigVO;
import com.admintem.web.entity.vo.PageResult;

import java.util.List;

/**
 * 系统参数配置服务。
 */
public interface SysAuthConfigService {

    PageResult<ConfigVO> pageQuery(ConfigPageQuery query);

    List<ConfigVO> listAll();

    Long create(ConfigSaveRequest request, String operator);

    void update(ConfigSaveRequest request, String operator);

    void delete(Long id, String operator);

    String getValue(String key);
}
