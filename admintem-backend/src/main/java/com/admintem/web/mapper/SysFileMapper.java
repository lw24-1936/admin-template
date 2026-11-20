package com.admintem.web.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.admintem.web.entity.SysFile;
import com.admintem.web.entity.dto.FilePageQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface SysFileMapper {

    IPage<SysFile> selectFilePage(IPage<SysFile> page, @Param("q") FilePageQuery q);

    SysFile selectById(@Param("id") Long id);

    int insert(SysFile file);

    int logicDelete(@Param("id") Long id, @Param("updateBy") String updateBy, @Param("now") LocalDateTime now);
}
