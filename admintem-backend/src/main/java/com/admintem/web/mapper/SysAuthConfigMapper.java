package com.admintem.web.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.admintem.web.entity.SysConfig;
import com.admintem.web.entity.dto.ConfigPageQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统参数配置 Mapper（sys_config）。
 */
public interface SysAuthConfigMapper {

    IPage<SysConfig> selectConfigPage(IPage<SysConfig> page, @Param("q") ConfigPageQuery q);

    List<SysConfig> selectAllOrdered();

    SysConfig selectById(@Param("id") Long id);

    SysConfig selectByKey(@Param("configKey") String configKey);

    long countByKey(@Param("configKey") String configKey, @Param("excludeId") Long excludeId);

    int insert(SysConfig config);

    int updateSelective(SysConfig config);

    int logicDelete(@Param("id") Long id, @Param("updateBy") String updateBy, @Param("now") LocalDateTime now);
}
